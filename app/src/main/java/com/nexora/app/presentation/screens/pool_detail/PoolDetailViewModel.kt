package com.nexora.app.presentation.screens.pool_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.nexora.app.core.common.Resource
import com.nexora.app.data.remote.dto.PoolItemData
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.domain.repository.PoolRepository
import com.nexora.app.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class PoolDetailState(
    val isLoading: Boolean = false,
    val poolId: String = "",
    val items: List<PoolItemData> = emptyList(),
    val error: String? = null
)

sealed class PoolDetailEvent {
    object RefreshItems : PoolDetailEvent()
    data class OnItemClick(val item: PoolItemData) : PoolDetailEvent()
}

sealed class PoolDetailEffect {
    data class ShowToast(val message: String) : PoolDetailEffect()
    data class OpenFile(val file: java.io.File, val mimeType: String) : PoolDetailEffect()
}

@HiltViewModel
class PoolDetailViewModel @Inject constructor(
    private val poolRepository: PoolRepository,
    private val discoveryRepository: DiscoveryRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<PoolDetailState, PoolDetailEvent, PoolDetailEffect>() {

    private val poolId: String = savedStateHandle.get<String>("poolId") ?: ""
    // Maps payloadId -> Triple(Payload, MimeType, FileName)
    private val activePayloads = mutableMapOf<Long, Triple<Payload, String, String>>()
    // Temporarily store the metadata of the last clicked item
    private var lastClickedItem: PoolItemData? = null

    init {
        setState { copy(poolId = poolId) }
        fetchItems()
        listenForIncomingFiles()
        listenForPayloadUpdates()
        listenForOfflineSync()
    }

    override fun createInitialState() = PoolDetailState()

    override fun handleEvent(event: PoolDetailEvent) {
        when (event) {
            PoolDetailEvent.RefreshItems -> fetchItems()
            is PoolDetailEvent.OnItemClick -> handleItemClick(event.item)
        }
    }

    private fun fetchItems() {
        if (poolId.isBlank()) return

        poolRepository.fetchPoolItems(poolId).onEach { resource ->
            when (resource) {
                is Resource.Loading -> setState { copy(isLoading = currentState.items.isEmpty(), error = null) }
                is Resource.Error -> setState { copy(isLoading = false, error = resource.message) }
                is Resource.Success -> {
                    Timber.d("Fetched ${resource.data.size} items for pool $poolId")
                    setState { copy(isLoading = false, items = resource.data, error = null) }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleItemClick(item: PoolItemData) {
        val endpointId = discoveryRepository.getConnectedEndpointId()
        if (endpointId == null) {
            setEffect { PoolDetailEffect.ShowToast("Not connected to any peer. Ensure host is nearby.") }
            return
        }

        lastClickedItem = item
        setEffect { PoolDetailEffect.ShowToast("Requesting ${item.itemName} from host...") }
        
        val request = JSONObject().apply {
            put("type", "REQUEST_FILE")
            put("uri", item.localPath)
            put("itemId", item.itemId)
        }
        discoveryRepository.sendMessage(endpointId, request.toString())
    }

    private fun listenForIncomingFiles() {
        discoveryRepository.getIncomingPayloads().onEach { (_, payload) ->
            if (payload.type == Payload.Type.FILE || payload.type == Payload.Type.STREAM) {
                // Associate the incoming payload with the metadata of the item the user just clicked
                val mimeType = lastClickedItem?.mimeType ?: "*/*"
                val fileName = lastClickedItem?.itemName ?: "received_file_${System.currentTimeMillis()}"
                activePayloads[payload.id] = Triple(payload, mimeType, fileName)
                lastClickedItem = null // Reset for next click
            }
        }.launchIn(viewModelScope)
    }

    private fun listenForPayloadUpdates() {
        discoveryRepository.getPayloadUpdates().onEach { (_, update) ->
            val payloadTriple = activePayloads[update.payloadId] ?: return@onEach
            
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    val originalPayload = payloadTriple.first
                    val mimeType = payloadTriple.second
                    val fileName = payloadTriple.third
                    
                    val finalizedFile = discoveryRepository.finalizePayload(originalPayload, fileName)
                    if (finalizedFile != null) {
                        setEffect { PoolDetailEffect.ShowToast("Fetched $fileName successfully!") }
                        setEffect { PoolDetailEffect.OpenFile(finalizedFile, mimeType) }
                    } else {
                        setEffect { PoolDetailEffect.ShowToast("Error saving received file") }
                    }
                    activePayloads.remove(update.payloadId)
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    setEffect { PoolDetailEffect.ShowToast("Failed to fetch item from host.") }
                    activePayloads.remove(update.payloadId)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun listenForOfflineSync() {
        discoveryRepository.incomingMessages.onEach { (_, message) ->
            try {
                val json = JSONObject(message)
                if (json.getString("type") == "SYNC_ITEMS") {
                    val itemsArray = json.getJSONArray("items")
                    val items = mutableListOf<PoolItemData>()
                    for (i in 0 until itemsArray.length()) {
                        val itemJson = itemsArray.getJSONObject(i)
                        items.add(PoolItemData(
                            itemId = itemJson.getString("itemId"),
                            poolId = itemJson.getString("poolId"),
                            itemName = itemJson.getString("itemName"),
                            itemType = itemJson.getString("itemType"),
                            mimeType = itemJson.getString("mimeType"),
                            size = itemJson.getLong("size"),
                            localPath = itemJson.getString("localPath")
                        ))
                    }
                    if (items.isNotEmpty()) {
                        Timber.d("Received ${items.size} items via P2P sync")
                        setState { copy(items = items, isLoading = false, error = null) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing offline sync message")
            }
        }.launchIn(viewModelScope)
    }
}

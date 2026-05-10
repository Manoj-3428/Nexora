package com.nexora.app.presentation.screens.active_pool

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.nearby.connection.Payload
import com.nexora.app.core.common.Resource
import com.nexora.app.data.remote.dto.PoolItemData
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.domain.repository.PoolRepository
import com.nexora.app.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

data class ActivePoolState(
    val poolName: String = "",
    val connectedPeers: List<String> = emptyList()
)

sealed class ActivePoolEvent {
    data class OnPeerConnected(val endpointId: String) : ActivePoolEvent()
    data class OnPeerDisconnected(val endpointId: String) : ActivePoolEvent()
}

@HiltViewModel
class ActivePoolViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository,
    private val poolRepository: PoolRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : BaseViewModel<ActivePoolState, ActivePoolEvent, Unit>() {

    private val poolId: String = savedStateHandle.get<String>("poolId") ?: ""

    init {
        listenForRequests()
    }

    override fun createInitialState() = ActivePoolState()

    override fun handleEvent(event: ActivePoolEvent) {
        when (event) {
            is ActivePoolEvent.OnPeerConnected -> {
                setState { copy(connectedPeers = connectedPeers + event.endpointId) }
                broadcastItems(event.endpointId)
            }
            is ActivePoolEvent.OnPeerDisconnected -> setState { copy(connectedPeers = connectedPeers - event.endpointId) }
        }
    }

    private fun broadcastItems(endpointId: String) {
        if (poolId.isBlank()) return
        
        poolRepository.fetchPoolItems(poolId).onEach { resource ->
            if (resource is Resource.Success) {
                val json = JSONObject().apply {
                    put("type", "SYNC_ITEMS")
                    val itemsArray = org.json.JSONArray()
                    resource.data.forEach { item ->
                        itemsArray.put(JSONObject().apply {
                            put("id", item.itemId)
                            put("itemId", item.itemId)
                            put("poolId", item.poolId)
                            put("itemName", item.itemName)
                            put("itemType", item.itemType)
                            put("mimeType", item.mimeType)
                            put("size", item.size)
                            put("localPath", item.localPath)
                        })
                    }
                    put("items", itemsArray)
                }
                discoveryRepository.sendMessage(endpointId, json.toString())
            }
        }.launchIn(viewModelScope)
    }

    private fun listenForRequests() {
        discoveryRepository.incomingMessages.onEach { (endpointId, message) ->
            try {
                val json = JSONObject(message)
                when (json.getString("type")) {
                    "REQUEST_FILE" -> {
                        val uriString = json.getString("uri")
                        sendFile(endpointId, uriString)
                    }
                    "CLIENT_JOINED" -> {
                        // When a client joins, the host can send the pool info
                        // In a real app, you'd fetch current items from the repository
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing P2P message")
            }
        }.launchIn(viewModelScope)
    }

    private fun sendFile(endpointId: String, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val filePayload = Payload.fromFile(pfd)
                // In Phase 1, we send as FILE. To "not download", we'd use STREAM.
                // But for now, let's use FILE to ensure it works, and we'll tell the user
                // that it's being "fetched" directly.
                discoveryRepository.sendMessage(endpointId, JSONObject().apply {
                    put("type", "FILE_METADATA")
                    put("uri", uriString)
                    put("mimeType", context.contentResolver.getType(uri) ?: "*/*")
                }.toString())
                
                discoveryRepository.sendPayload(endpointId, filePayload)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending file")
        }
    }
}

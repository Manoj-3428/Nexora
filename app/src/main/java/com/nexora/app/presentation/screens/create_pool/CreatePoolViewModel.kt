package com.nexora.app.presentation.screens.create_pool

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.nexora.app.core.common.Resource
import com.nexora.app.data.remote.dto.AddPoolItemRequest
import com.nexora.app.data.remote.dto.CreatePoolRequest
import com.nexora.app.domain.model.PoolConfig
import com.nexora.app.domain.model.Visibility
import com.nexora.app.domain.model.SelectedFile
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.domain.repository.PoolRepository
import com.nexora.app.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class CreatePoolState(
    val isLoading: Boolean = false,
    val poolName: String = "",
    val visibility: Visibility = Visibility.EVERYONE,
    val password: String = "",
    val expiryHours: Int = 24,
    val selectedFiles: List<SelectedFile> = emptyList(),
    val error: String? = null,
    val isAdvertising: Boolean = false
)

sealed class CreatePoolEvent {
    data class OnNameChanged(val name: String) : CreatePoolEvent()
    data class OnVisibilityChanged(val visibility: Visibility) : CreatePoolEvent()
    data class OnPasswordChanged(val password: String) : CreatePoolEvent()
    data class OnExpiryChanged(val hours: Int) : CreatePoolEvent()
    data class OnItemsSelected(val files: List<SelectedFile>) : CreatePoolEvent()
    object OnStartAdvertising : CreatePoolEvent()
    object OnStopAdvertising : CreatePoolEvent()
}

sealed class CreatePoolEffect {
    data class ShowToast(val message: String) : CreatePoolEffect()
    object NavigateToActivePool : CreatePoolEffect()
}

@HiltViewModel
class CreatePoolViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository,
    private val poolRepository: PoolRepository
) : BaseViewModel<CreatePoolState, CreatePoolEvent, CreatePoolEffect>() {

    override fun createInitialState() = CreatePoolState()

    override fun handleEvent(event: CreatePoolEvent) {
        when (event) {
            is CreatePoolEvent.OnNameChanged -> setState { copy(poolName = event.name) }
            is CreatePoolEvent.OnVisibilityChanged -> setState { copy(visibility = event.visibility) }
            is CreatePoolEvent.OnPasswordChanged -> setState { copy(password = event.password) }
            is CreatePoolEvent.OnExpiryChanged -> setState { copy(expiryHours = event.hours) }
            is CreatePoolEvent.OnItemsSelected -> setState { copy(selectedFiles = event.files) }
            CreatePoolEvent.OnStartAdvertising -> startAdvertising()
            CreatePoolEvent.OnStopAdvertising -> stopAdvertising()
        }
    }

    private fun startAdvertising() {
        val state = currentState
        if (state.poolName.isBlank()) {
            setEffect { CreatePoolEffect.ShowToast("Please enter a pool name") }
            return
        }

        val expiresAt = ZonedDateTime.now().plusHours(state.expiryHours.toLong())
            .format(DateTimeFormatter.ISO_INSTANT)

        val createRequest = CreatePoolRequest(
            poolName = state.poolName,
            expiresAt = expiresAt,
            isPublic = state.visibility == Visibility.EVERYONE,
            passwordProtected = state.visibility == Visibility.RESTRICTED,
            password = if (state.visibility == Visibility.RESTRICTED) state.password else null,
            hostDeviceId = Build.MODEL,
            protocolType = "WIFI_DIRECT"
        )

        // Step 1: Create Pool on Backend
        poolRepository.createPool(createRequest).onEach { resource ->
            when (resource) {
                is Resource.Loading -> setState { copy(isLoading = true, error = null) }
                is Resource.Error -> {
                    setState { copy(isLoading = false, error = resource.message) }
                    setEffect { CreatePoolEffect.ShowToast("Backend Error: ${resource.message}") }
                }
                is Resource.Success -> {
                    val poolId = resource.data.poolId
                    addItemsAndStartNearby(poolId)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun addItemsAndStartNearby(poolId: String) {
        val state = currentState
        
        // Step 2: Add all selected items to backend
        val itemRequests = state.selectedFiles.map { file ->
            AddPoolItemRequest(
                itemName = file.name,
                itemType = file.type,
                mimeType = file.mimeType,
                size = file.size,
                localPath = file.uri,
                streamable = true
            )
        }

        viewModelScope.launch {
            try {
                // Add items sequentially and wait for each to succeed
                itemRequests.forEach { request ->
                    val result = poolRepository.addPoolItem(poolId, request)
                        .filter { it is Resource.Success || it is Resource.Error }
                        .first()
                    
                    if (result is Resource.Error) {
                        throw Exception(result.message)
                    }
                }

                // Step 3: Start Nearby Advertising
                val config = PoolConfig(
                    poolId = poolId,
                    name = state.poolName,
                    visibility = state.visibility,
                    password = if (state.visibility == Visibility.RESTRICTED) state.password else null,
                    expiryDurationHours = state.expiryHours,
                    selectedItemUris = state.selectedFiles.map { it.uri }
                )

                discoveryRepository.startAdvertising(config).onEach { nearbyResource ->
                    when (nearbyResource) {
                        is Resource.Success -> {
                            setState { copy(isLoading = false, isAdvertising = true) }
                            setEffect { CreatePoolEffect.NavigateToActivePool }
                        }
                        is Resource.Error -> {
                            setState { copy(isLoading = false, error = nearbyResource.message) }
                            setEffect { CreatePoolEffect.ShowToast("Nearby Error: ${nearbyResource.message}") }
                        }
                        Resource.Loading -> {}
                    }
                }.launchIn(viewModelScope)

            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                setEffect { CreatePoolEffect.ShowToast("Error adding items: ${e.message}") }
            }
        }
    }

    private fun stopAdvertising() {
        discoveryRepository.stopAdvertising()
        setState { copy(isAdvertising = false) }
    }
}

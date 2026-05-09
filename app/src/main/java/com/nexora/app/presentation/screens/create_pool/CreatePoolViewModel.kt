package com.nexora.app.presentation.screens.create_pool

import androidx.lifecycle.viewModelScope
import com.nexora.app.core.common.Resource
import com.nexora.app.domain.model.PoolConfig
import com.nexora.app.domain.model.Visibility
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class CreatePoolState(
    val isLoading: Boolean = false,
    val poolName: String = "",
    val visibility: Visibility = Visibility.EVERYONE,
    val password: String = "",
    val expiryHours: Int = 24,
    val selectedItemCount: Int = 0,
    val error: String? = null,
    val isAdvertising: Boolean = false
)

sealed class CreatePoolEvent {
    data class OnNameChanged(val name: String) : CreatePoolEvent()
    data class OnVisibilityChanged(val visibility: Visibility) : CreatePoolEvent()
    data class OnPasswordChanged(val password: String) : CreatePoolEvent()
    data class OnExpiryChanged(val hours: Int) : CreatePoolEvent()
    data class OnItemsSelected(val count: Int) : CreatePoolEvent()
    object OnStartAdvertising : CreatePoolEvent()
    object OnStopAdvertising : CreatePoolEvent()
}

sealed class CreatePoolEffect {
    data class ShowToast(val message: String) : CreatePoolEffect()
    object NavigateToActivePool : CreatePoolEffect()
}

@HiltViewModel
class CreatePoolViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository
) : BaseViewModel<CreatePoolState, CreatePoolEvent, CreatePoolEffect>() {

    override fun createInitialState() = CreatePoolState()

    override fun handleEvent(event: CreatePoolEvent) {
        when (event) {
            is CreatePoolEvent.OnNameChanged -> setState { copy(poolName = event.name) }
            is CreatePoolEvent.OnVisibilityChanged -> setState { copy(visibility = event.visibility) }
            is CreatePoolEvent.OnPasswordChanged -> setState { copy(password = event.password) }
            is CreatePoolEvent.OnExpiryChanged -> setState { copy(expiryHours = event.hours) }
            is CreatePoolEvent.OnItemsSelected -> setState { copy(selectedItemCount = event.count) }
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

        val config = PoolConfig(
            name = state.poolName,
            visibility = state.visibility,
            password = if (state.visibility == Visibility.RESTRICTED) state.password else null,
            expiryDurationHours = state.expiryHours,
            selectedItemUris = List(state.selectedItemCount) { "item_$it" } // Mocked for now
        )

        discoveryRepository.startAdvertising(config).onEach { resource ->
            when (resource) {
                is Resource.Error -> {
                    setState { copy(isLoading = false, error = resource.message) }
                    setEffect { CreatePoolEffect.ShowToast(resource.message) }
                }
                Resource.Loading -> setState { copy(isLoading = true, error = null) }
                is Resource.Success -> {
                    setState { copy(isLoading = false, isAdvertising = true) }
                    setEffect { CreatePoolEffect.NavigateToActivePool }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun stopAdvertising() {
        discoveryRepository.stopAdvertising()
        setState { copy(isAdvertising = false) }
    }
}

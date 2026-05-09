package com.nexora.app.presentation.screens.discovery

import androidx.lifecycle.viewModelScope
import com.nexora.app.domain.model.SharingPool
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class DiscoveryState(
    val isScanning: Boolean = false,
    val discoveredPools: List<SharingPool> = emptyList(),
    val error: String? = null
)

sealed class DiscoveryEvent {
    object StartScanning : DiscoveryEvent()
    object StopScanning : DiscoveryEvent()
    data class JoinPool(val poolId: String, val password: String?) : DiscoveryEvent()
}

sealed class DiscoveryEffect {
    data class ShowToast(val message: String) : DiscoveryEffect()
    object NavigateToPoolDetail : DiscoveryEffect()
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository
) : BaseViewModel<DiscoveryState, DiscoveryEvent, DiscoveryEffect>() {

    override fun createInitialState() = DiscoveryState()

    override fun handleEvent(event: DiscoveryEvent) {
        when (event) {
            DiscoveryEvent.StartScanning -> startScanning()
            DiscoveryEvent.StopScanning -> stopScanning()
            is DiscoveryEvent.JoinPool -> joinPool(event.poolId, event.password)
        }
    }

    private fun startScanning() {
        setState { copy(isScanning = true) }
        discoveryRepository.startDiscovery().onEach { pools ->
            setState { copy(discoveredPools = pools) }
        }.launchIn(viewModelScope)
    }

    private fun stopScanning() {
        discoveryRepository.stopDiscovery()
        setState { copy(isScanning = false, discoveredPools = emptyList()) }
    }

    private fun joinPool(poolId: String, password: String?) {
        // Mock join implementation for now, Phase 3 will handle true streaming
        setEffect { DiscoveryEffect.ShowToast("Joining pool: $poolId...") }
        // discoveryRepository.connectToPool(poolId, password)
    }
}

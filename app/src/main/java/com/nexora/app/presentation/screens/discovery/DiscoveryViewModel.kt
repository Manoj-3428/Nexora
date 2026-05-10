package com.nexora.app.presentation.screens.discovery

import androidx.lifecycle.viewModelScope
import com.nexora.app.core.common.Resource
import com.nexora.app.domain.model.SharingPool
import com.nexora.app.domain.repository.DiscoveryRepository
import com.nexora.app.domain.repository.PoolRepository
import com.nexora.app.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class DiscoveryState(
    val isScanning: Boolean = false,
    val isLoading: Boolean = false,
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
    data class NavigateToPoolDetail(val poolId: String) : DiscoveryEffect()
}

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val discoveryRepository: DiscoveryRepository,
    private val poolRepository: PoolRepository
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
        
        val backendFlow = poolRepository.fetchNearbyPools()
        val localFlow = discoveryRepository.startDiscovery()
        
        combine(backendFlow, localFlow) { backendResource, localPools ->
            val pools = mutableListOf<SharingPool>()
            
            // Add local P2P pools
            pools.addAll(localPools)
            
            // Add backend coordinated pools (preventing duplicates)
            if (backendResource is Resource.Success) {
                backendResource.data.forEach { poolData ->
                    if (pools.none { it.id == poolData.poolId }) {
                        pools.add(
                            SharingPool(
                                id = poolData.poolId,
                                name = poolData.poolName,
                                ownerName = "Host: ${poolData.hostDeviceId}",
                                isProtected = poolData.passwordProtected,
                                itemCount = poolData.totalFiles, 
                                expiryTimestamp = 0L 
                            )
                        )
                    }
                }
            }
            pools
        }.onEach { mergedPools ->
            setState { copy(discoveredPools = mergedPools) }
        }.launchIn(viewModelScope)
    }

    private fun stopScanning() {
        discoveryRepository.stopDiscovery()
        setState { copy(isScanning = false, discoveredPools = emptyList()) }
    }

    private fun joinPool(poolId: String, password: String?) {
        poolRepository.joinPool(poolId).onEach { resource ->
            when (resource) {
                is Resource.Loading -> setState { copy(isLoading = true) }
                is Resource.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect { DiscoveryEffect.ShowToast("Backend Join Failed: ${resource.message}") }
                }
                is Resource.Success -> {
                    // Backend join successful, now connect locally if available
                    discoveryRepository.connectToPool(poolId, password).onEach { nearbyResource ->
                        when (nearbyResource) {
                            is Resource.Loading -> {}
                            is Resource.Error -> {
                                setState { copy(isLoading = false) }
                                setEffect { DiscoveryEffect.ShowToast("Nearby Connection Error: ${nearbyResource.message}") }
                            }
                            is Resource.Success -> {
                                setState { copy(isLoading = false) }
                                setEffect { DiscoveryEffect.ShowToast("Successfully joined pool!") }
                                setEffect { DiscoveryEffect.NavigateToPoolDetail(poolId) }
                            }
                        }
                    }.launchIn(viewModelScope)
                }
            }
        }.launchIn(viewModelScope)
    }
}

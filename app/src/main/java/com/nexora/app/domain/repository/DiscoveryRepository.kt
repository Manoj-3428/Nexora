package com.nexora.app.domain.repository

import com.nexora.app.core.common.Resource
import com.nexora.app.domain.model.PoolConfig
import com.nexora.app.domain.model.SharingPool
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {
    
    // Sender methods
    fun startAdvertising(config: PoolConfig): Flow<Resource<Unit>>
    fun stopAdvertising()
    
    // Receiver methods
    fun startDiscovery(): Flow<List<SharingPool>>
    fun stopDiscovery()
    
    fun connectToPool(poolId: String, password: String?): Flow<Resource<Unit>>
}

package com.nexora.app.domain.repository

import com.nexora.app.core.common.Resource
import com.nexora.app.domain.model.PoolConfig
import com.nexora.app.domain.model.SharingPool
import com.google.android.gms.nearby.connection.Payload
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {
    
    // Sender methods
    fun startAdvertising(config: PoolConfig): Flow<Resource<Unit>>
    fun stopAdvertising()
    
    // Receiver methods
    fun startDiscovery(): Flow<List<SharingPool>>
    fun stopDiscovery()
    fun connectToPool(poolId: String, password: String? = null): Flow<Resource<Unit>>
    fun sendMessage(endpointId: String, message: String)
    fun sendPayload(endpointId: String, payload: com.google.android.gms.nearby.connection.Payload)
    fun getIncomingPayloads(): Flow<Pair<String, Payload>>
    fun getPayloadUpdates(): Flow<Pair<String, com.google.android.gms.nearby.connection.PayloadTransferUpdate>>
    fun getConnectedEndpointId(): String?
    fun finalizePayload(payload: Payload, fileName: String): java.io.File?
    
    // P2P Messaging for offline sync
    val incomingMessages: Flow<Pair<String, String>>
}

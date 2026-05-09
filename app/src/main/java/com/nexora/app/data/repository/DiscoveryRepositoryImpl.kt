package com.nexora.app.data.repository

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.nexora.app.core.common.BaseRepository
import com.nexora.app.core.common.Resource
import com.nexora.app.domain.model.PoolConfig
import com.nexora.app.domain.model.SharingPool
import com.nexora.app.domain.repository.DiscoveryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class DiscoveryRepositoryImpl(private val context: Context) : BaseRepository(), DiscoveryRepository {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.nexora.app.SERVICE_ID"
    private val strategy = Strategy.P2P_STAR

    private val discoveredPools = MutableStateFlow<Map<String, SharingPool>>(emptyMap())

    override fun startAdvertising(config: PoolConfig): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        // Here we can serialize PoolConfig into the endpoint name so discoverers can read metadata before connecting
        val endpointName = "${config.name}|${config.visibility.name}|${config.selectedItemUris.size}" // simplified

        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                Timber.d("Connection initiated by $endpointId")
                // For Phase 2, auto-accept. In reality, you'd check passwords here via initial payloads.
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    Timber.d("Connection successful with $endpointId")
                } else {
                    Timber.e("Connection failed with $endpointId")
                }
            }

            override fun onDisconnected(endpointId: String) {
                Timber.d("Disconnected from $endpointId")
            }
        }

        try {
            connectionsClient.startAdvertising(
                endpointName,
                serviceId,
                connectionLifecycleCallback,
                advertisingOptions
            ).addOnSuccessListener {
                Timber.d("Advertising started successfully")
            }.addOnFailureListener {
                Timber.e(it, "Advertising failed")
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to start advertising: ${e.message}", e))
        }
    }

    override fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
    }

    override fun startDiscovery(): Flow<List<SharingPool>> = callbackFlow {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()

        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                Timber.d("Endpoint found: $endpointId - ${info.endpointName}")
                // Parse the serialized pool info from endpointName
                val parts = info.endpointName.split("|")
                if (parts.size >= 3) {
                    val pool = SharingPool(
                        id = endpointId,
                        name = parts[0],
                        ownerName = "Unknown", // Assuming not passed in simplified version
                        isProtected = parts[1] == "RESTRICTED",
                        itemCount = parts[2].toIntOrNull() ?: 0,
                        expiryTimestamp = 0L
                    )
                    
                    val currentPools = discoveredPools.value.toMutableMap()
                    currentPools[endpointId] = pool
                    discoveredPools.value = currentPools
                    trySend(currentPools.values.toList())
                }
            }

            override fun onEndpointLost(endpointId: String) {
                Timber.d("Endpoint lost: $endpointId")
                val currentPools = discoveredPools.value.toMutableMap()
                currentPools.remove(endpointId)
                discoveredPools.value = currentPools
                trySend(currentPools.values.toList())
            }
        }

        connectionsClient.startDiscovery(
            serviceId,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Timber.d("Discovery started")
        }.addOnFailureListener {
            Timber.e(it, "Discovery failed")
        }

        awaitClose {
            stopDiscovery()
        }
    }

    override fun stopDiscovery() {
        connectionsClient.stopDiscovery()
    }

    override fun connectToPool(poolId: String, password: String?): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        // Implementation for connecting to the endpoint and verifying password
        // This involves sending a payload and waiting for an ACK.
        emit(Resource.Success(Unit))
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // Handle incoming data/streams
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Handle transfer progress
        }
    }
}

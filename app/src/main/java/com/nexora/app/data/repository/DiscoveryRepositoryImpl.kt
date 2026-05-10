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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class DiscoveryRepositoryImpl(private val context: Context) : BaseRepository(), DiscoveryRepository {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.nexora.app.SERVICE_ID"
    private val strategy = Strategy.P2P_STAR

    private val discoveredPools = MutableStateFlow<Map<String, SharingPool>>(emptyMap())
    private val incomingPayloads = MutableSharedFlow<Pair<String, Payload>>()
    private val payloadUpdates = MutableSharedFlow<Pair<String, PayloadTransferUpdate>>()
    private var connectedEndpointId: String? = null

    override fun startAdvertising(config: PoolConfig): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        // Here we can serialize PoolConfig into the endpoint name so discoverers can read metadata before connecting
        val endpointName = "${config.poolId}|${config.name}|${config.visibility.name}|${config.selectedItemUris.size}" 

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
                if (parts.size >= 4) {
                    val pool = SharingPool(
                        id = parts[0], // Use backend poolId as the primary ID
                        name = parts[1],
                        ownerName = "Nearby Peer",
                        isProtected = parts[2] == "RESTRICTED",
                        itemCount = parts[3].toIntOrNull() ?: 0,
                        expiryTimestamp = 0L
                    )
                    
                    val currentPools = discoveredPools.value.toMutableMap()
                    currentPools[endpointId] = pool // Still map by endpointId for local tracking
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

    override fun connectToPool(poolId: String, password: String?): Flow<Resource<Unit>> = callbackFlow {
        trySend(Resource.Loading)
        
        val endpointId = discoveredPools.value.entries.find { it.value.id == poolId }?.key
        if (endpointId == null) {
            trySend(Resource.Error("Local pool endpoint not found. Ensure you are close to the host."))
            close()
            return@callbackFlow
        }

        val lifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                Timber.d("Connection initiated with $endpointId, accepting...")
                connectionsClient.acceptConnection(endpointId, payloadCallback)
                    .addOnFailureListener { e ->
                        trySend(Resource.Error("Failed to accept connection: ${e.message}"))
                        close()
                    }
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    Timber.d("Connection successful with $endpointId")
                    connectedEndpointId = endpointId
                    trySend(Resource.Success(Unit))
                    // We don't close the flow yet because we want to keep the connection until disconnected?
                    // Actually for Resource flows, one success is usually enough.
                    close()
                } else {
                    Timber.e("Connection failed with $endpointId: ${result.status.statusMessage}")
                    trySend(Resource.Error("Connection failed: ${result.status.statusMessage ?: "Unknown error"}"))
                    close()
                }
            }

            override fun onDisconnected(endpointId: String) {
                Timber.d("Disconnected from $endpointId")
                if (connectedEndpointId == endpointId) connectedEndpointId = null
            }
        }

        connectionsClient.requestConnection("User", endpointId, lifecycleCallback)
            .addOnFailureListener { e ->
                Timber.e(e, "Request connection failed")
                trySend(Resource.Error("Nearby request failed: ${e.message}"))
                close()
            }

        awaitClose { 
            // Don't disconnect here as we want to maintain the connection for data transfer
        }
    }

    private val _incomingMessages = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 64)
    override val incomingMessages: Flow<Pair<String, String>> = _incomingMessages.asSharedFlow()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.FILE -> {
                    Timber.d("Received file payload from $endpointId")
                    repositoryScope.launch {
                        incomingPayloads.emit(endpointId to payload)
                    }
                }
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes()
                    if (bytes != null) {
                        val message = String(bytes, Charsets.UTF_8)
                        Timber.d("Received message from $endpointId: $message")
                        _incomingMessages.tryEmit(endpointId to message)
                    }
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            repositoryScope.launch {
                payloadUpdates.emit(endpointId to update)
            }
        }
    }

    override fun sendMessage(endpointId: String, message: String) {
        val payload = Payload.fromBytes(message.toByteArray(Charsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
            .addOnFailureListener { Timber.e(it, "Failed to send message to $endpointId") }
    }

    override fun sendPayload(endpointId: String, payload: Payload) {
        connectionsClient.sendPayload(endpointId, payload)
            .addOnFailureListener { Timber.e(it, "Failed to send payload to $endpointId") }
    }

    override fun getIncomingPayloads(): Flow<Pair<String, Payload>> = incomingPayloads.asSharedFlow()

    override fun getPayloadUpdates(): Flow<Pair<String, PayloadTransferUpdate>> = payloadUpdates.asSharedFlow()

    override fun getConnectedEndpointId(): String? = connectedEndpointId

    private val repositoryScope = MainScope()

    override fun finalizePayload(payload: Payload, fileName: String): java.io.File? {
        val payloadFile = payload.asFile() ?: return null
        
        return try {
            val receivedDir = java.io.File(context.cacheDir, "received")
            if (!receivedDir.exists()) receivedDir.mkdirs()
            
            val targetFile = java.io.File(receivedDir, fileName)
            val finalFile = if (targetFile.exists()) {
                java.io.File(receivedDir, "${System.currentTimeMillis()}_$fileName")
            } else {
                targetFile
            }

            // Try rename first (fastest)
            val javaFile = payloadFile.asJavaFile()
            if (javaFile != null && javaFile.renameTo(finalFile)) {
                Timber.d("File renamed to: ${finalFile.absolutePath}")
                return finalFile
            }

            // Fallback to ParcelFileDescriptor which is more reliable on Android 10+
            val pfd = payloadFile.asParcelFileDescriptor()
            if (pfd != null) {
                java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                    java.io.FileOutputStream(finalFile).use { output ->
                        input.copyTo(output)
                    }
                }
                pfd.close()
                // Try to delete the temporary file if it was a java file
                javaFile?.delete()
                Timber.d("File copied via PFD to: ${finalFile.absolutePath}")
                finalFile
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finalizing payload")
            null
        }
    }
}

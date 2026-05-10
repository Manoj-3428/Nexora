package com.nexora.app.data.remote.dto

data class CreatePoolRequest(
    val poolName: String,
    val expiresAt: String, // ISO 8601
    val isPublic: Boolean = true,
    val passwordProtected: Boolean = false,
    val password: String? = null,
    val hostDeviceId: String,
    val protocolType: String = "WIFI_DIRECT"
)

data class AddPoolItemRequest(
    val itemName: String,
    val itemType: String, // VIDEO, AUDIO, DOCUMENT, IMAGE
    val mimeType: String,
    val size: Long,
    val duration: Long? = null,
    val thumbnail: String? = null,
    val localPath: String? = null,
    val checksumHash: String? = null,
    val streamUrl: String? = null,
    val streamable: Boolean = true
)

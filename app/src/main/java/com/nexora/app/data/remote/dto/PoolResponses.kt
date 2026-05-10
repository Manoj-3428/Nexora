package com.nexora.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PoolResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    val pool: PoolData?
)

data class PoolData(
    val poolId: String,
    val poolName: String,
    val expiresAt: String,
    val isPublic: Boolean,
    val passwordProtected: Boolean,
    val hostDeviceId: String,
    val protocolType: String,
    val poolStatus: String,
    val totalFiles: Int,
    val totalSize: Long
)

data class PoolListResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    val pools: List<PoolData>
)

data class PoolItemResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    val item: PoolItemData?
)

data class PoolItemListResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    val items: List<PoolItemData>
)

data class PoolItemData(
    val itemId: String,
    val poolId: String,
    val itemName: String,
    val itemType: String,
    val mimeType: String,
    val size: Long,
    val localPath: String
)

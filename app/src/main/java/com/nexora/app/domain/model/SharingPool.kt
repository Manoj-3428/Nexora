package com.nexora.app.domain.model

data class SharingPool(
    val id: String,
    val ownerName: String,
    val name: String,
    val isProtected: Boolean,
    val expiryTimestamp: Long,
    val itemCount: Int
)

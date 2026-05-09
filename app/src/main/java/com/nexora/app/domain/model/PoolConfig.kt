package com.nexora.app.domain.model

enum class Visibility {
    EVERYONE,
    RESTRICTED
}

data class PoolConfig(
    val name: String,
    val visibility: Visibility,
    val password: String? = null,
    val expiryDurationHours: Int = 24,
    val selectedItemUris: List<String> = emptyList()
)

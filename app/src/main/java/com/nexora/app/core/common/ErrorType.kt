package com.nexora.app.core.common

enum class ErrorType {
    NETWORK_ERROR,
    UNAUTHORIZED,
    TIMEOUT,
    NOT_FOUND,
    NEARBY_CONNECTION_FAILED,
    NEARBY_DISCONNECTED,
    UNKNOWN_ERROR
}

data class AppError(
    val type: ErrorType,
    val message: String? = null,
    val exception: Throwable? = null
)

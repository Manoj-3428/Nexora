package com.nexora.app.core.network

sealed class NetworkResult<T>(
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
) {
    class Success<T>(data: T, message: String? = null) : NetworkResult<T>(data, message)
    class Error<T>(message: String?, error: String? = null, data: T? = null) : NetworkResult<T>(data, message, error)
    class Loading<T> : NetworkResult<T>()
}

package com.nexora.app.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

abstract class BaseRepository {

    /**
     * Executes a given suspend block safely on a specific dispatcher.
     * Maps common exceptions (IO, HTTP) into our standardized Resource wrapper.
     */
    protected suspend fun <T> safeCall(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        apiCall: suspend () -> T
    ): Resource<T> {
        return withContext(dispatcher) {
            try {
                Resource.Success(apiCall.invoke())
            } catch (throwable: Throwable) {
                when (throwable) {
                    is IOException -> Resource.Error(
                        message = "Network error. Please check your connection.",
                        exception = throwable
                    )
                    is HttpException -> {
                        val code = throwable.code()
                        val errorMsg = throwable.response()?.errorBody()?.string()
                        Resource.Error(
                            message = "Server error: $code - $errorMsg",
                            exception = throwable
                        )
                    }
                    else -> Resource.Error(
                        message = throwable.localizedMessage ?: "Unknown error occurred",
                        exception = throwable as? Exception
                    )
                }
            }
        }
    }
}

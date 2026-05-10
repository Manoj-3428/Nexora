package com.nexora.app.data.repository

import com.nexora.app.core.network.NetworkResult
import com.nexora.app.core.network.TokenManager
import com.nexora.app.data.remote.AuthApi
import com.nexora.app.data.remote.dto.LoginRequest
import com.nexora.app.data.remote.dto.RegisterRequest
import com.nexora.app.domain.repository.AuthRepository
import android.util.Log
import java.net.SocketTimeoutException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {
    override suspend fun login(request: LoginRequest): NetworkResult<Unit> {
        return try {
            val response = authApi.login(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val token = response.body()?.data?.token
                if (token != null) {
                    tokenManager.saveToken(token)
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error("Authentication successful but token is missing.")
                }
            } else {
                NetworkResult.Error(response.body()?.message ?: "Login failed. Please check your credentials.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error", e)
            handleException(e)
        }
    }

    override suspend fun register(request: RegisterRequest): NetworkResult<Unit> {
         return try {
            val response = authApi.register(request)
            if (response.isSuccessful && response.body()?.success == true) {
                NetworkResult.Success(Unit)
            } else {
                NetworkResult.Error(response.body()?.message ?: "Registration failed. Please try again.")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration error", e)
            handleException(e)
        }
    }

    private fun handleException(e: Exception): NetworkResult<Unit> {
        return when (e) {
            is SocketTimeoutException -> NetworkResult.Error("Connection timed out. Please try again.")
            else -> NetworkResult.Error(e.localizedMessage ?: "An unexpected error occurred.")
        }
    }
}

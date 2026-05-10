package com.nexora.app.domain.repository

import com.nexora.app.core.network.NetworkResult
import com.nexora.app.data.remote.dto.LoginRequest
import com.nexora.app.data.remote.dto.RegisterRequest

interface AuthRepository {
    suspend fun login(request: LoginRequest): NetworkResult<Unit>
    suspend fun register(request: RegisterRequest): NetworkResult<Unit>
}

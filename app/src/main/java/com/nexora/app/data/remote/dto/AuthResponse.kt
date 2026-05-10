package com.nexora.app.data.remote.dto

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val data: AuthData?,
    val error: String?
)

data class AuthData(
    val user: UserDto,
    val token: String
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val visibility: String,
    val isOnline: Boolean
)

package com.nexora.app.data.remote.dto

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val deviceId: String
)

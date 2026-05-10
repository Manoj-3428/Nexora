package com.nexora.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.app.core.network.NetworkResult
import com.nexora.app.core.network.TokenManager
import com.nexora.app.data.remote.dto.LoginRequest
import com.nexora.app.data.remote.dto.RegisterRequest
import com.nexora.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Patterns
import android.util.Log
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null,
    val confirmPasswordError: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthState())
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.update { AuthState() }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun login(email: String, pass: String) {
        val isEmailValid = isValidEmail(email)
        val isPassValid = pass.length >= 6

        _uiState.update { 
            it.copy(
                emailError = if (isEmailValid) null else "Invalid email format",
                passwordError = if (isPassValid) null else "Password must be at least 6 characters"
            )
        }

        if (!isEmailValid || !isPassValid) return

        viewModelScope.launch {
            Log.d("AuthViewModel", "Attempting login for email: $email")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val deviceId = tokenManager.getOrCreateDeviceId()
            
            when (val result = repository.login(LoginRequest(email, pass, deviceId))) {
                is NetworkResult.Success -> {
                    Log.d("AuthViewModel", "Login successful")
                    _uiState.update { it.copy(isLoading = false, success = true) }
                }
                is NetworkResult.Error -> {
                    Log.e("AuthViewModel", "Login failed: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { } // Unused
            }
        }
    }

    fun register(name: String, email: String, pass: String, confirmPass: String) {
        val isNameValid = name.isNotBlank()
        val isEmailValid = isValidEmail(email)
        val isPassValid = pass.length >= 6
        val isConfirmPassValid = pass == confirmPass

        _uiState.update { 
            it.copy(
                nameError = if (isNameValid) null else "Name cannot be empty",
                emailError = if (isEmailValid) null else "Invalid email format",
                passwordError = if (isPassValid) null else "Password must be at least 6 characters",
                confirmPasswordError = if (isConfirmPassValid) null else "Passwords do not match"
            )
        }

        if (!isNameValid || !isEmailValid || !isPassValid || !isConfirmPassValid) return

        viewModelScope.launch {
            Log.d("AuthViewModel", "Attempting registration for email: $email")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val deviceId = tokenManager.getOrCreateDeviceId()
            
            when (val result = repository.register(RegisterRequest(name, email, pass, deviceId))) {
                is NetworkResult.Success -> {
                    Log.d("AuthViewModel", "Registration successful, auto-logging in")
                    login(email, pass)
                }
                is NetworkResult.Error -> {
                    Log.e("AuthViewModel", "Registration failed: ${result.message}")
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is NetworkResult.Loading -> { }
            }
        }
    }
}

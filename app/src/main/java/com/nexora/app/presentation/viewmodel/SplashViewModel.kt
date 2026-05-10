package com.nexora.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.nexora.app.core.network.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {
    val tokenFlow = tokenManager.getTokenFlow()
}

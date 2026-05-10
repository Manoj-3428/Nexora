package com.nexora.app.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexora.app.core.designsystem.components.NexoraButton
import com.nexora.app.core.designsystem.components.NexoraTextButton
import com.nexora.app.core.designsystem.components.NexoraTextField
import com.nexora.app.presentation.viewmodel.AuthViewModel

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            onSignupSuccess()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Join Nexora for seamless sharing",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        NexoraTextField(
            value = name,
            onValueChange = { name = it },
            label = "Full Name",
            isError = uiState.nameError != null,
            errorMessage = uiState.nameError
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        NexoraTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            isError = uiState.emailError != null,
            errorMessage = uiState.emailError
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        NexoraTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        NexoraTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            isPassword = true,
            isError = uiState.confirmPasswordError != null,
            errorMessage = uiState.confirmPasswordError
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        NexoraButton(
            text = "Sign Up",
            onClick = { viewModel.register(name, email, password, confirmPassword) },
            isLoading = uiState.isLoading
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            NexoraTextButton(text = "Log In", onClick = onNavigateToLogin)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

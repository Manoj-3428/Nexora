package com.nexora.app.presentation.screens.create_pool

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexora.app.core.designsystem.theme.Dimens
import com.nexora.app.domain.model.Visibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePoolScreen(
    viewModel: CreatePoolViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToActivePool: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launchers for Runtime Permissions required by Nearby Connections API
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.handleEvent(CreatePoolEvent.OnStartAdvertising)
        } else {
            Toast.makeText(context, "Permissions required to create pool.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreatePoolEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                CreatePoolEffect.NavigateToActivePool -> {
                    onNavigateToActivePool()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Sharing Pool") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.SpaceMedium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMedium)
        ) {
            // Pool Name
            OutlinedTextField(
                value = state.poolName,
                onValueChange = { viewModel.handleEvent(CreatePoolEvent.OnNameChanged(it)) },
                label = { Text("Pool Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            )

            // Visibility
            Text(
                text = "Visibility",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSmall)
            ) {
                FilterChip(
                    selected = state.visibility == Visibility.EVERYONE,
                    onClick = { viewModel.handleEvent(CreatePoolEvent.OnVisibilityChanged(Visibility.EVERYONE)) },
                    label = { Text("Everyone") }
                )
                FilterChip(
                    selected = state.visibility == Visibility.RESTRICTED,
                    onClick = { viewModel.handleEvent(CreatePoolEvent.OnVisibilityChanged(Visibility.RESTRICTED)) },
                    label = { Text("Restricted") }
                )
            }

            // Password (if restricted)
            if (state.visibility == Visibility.RESTRICTED) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { viewModel.handleEvent(CreatePoolEvent.OnPasswordChanged(it)) },
                    label = { Text("Pool Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Dummy Item Selection
            Button(
                onClick = {
                    viewModel.handleEvent(CreatePoolEvent.OnItemsSelected(state.selectedItemCount + 1))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("Select Files/Media (${state.selectedItemCount} selected)", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Check permissions before advertising
                    val permissionsToRequest = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                    }
                    permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading && state.poolName.isNotBlank(),
                shape = RoundedCornerShape(Dimens.CornerMedium)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Start Pool", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

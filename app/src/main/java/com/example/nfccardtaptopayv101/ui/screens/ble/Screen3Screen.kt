package com.example.nfccardtaptopayv101.ui.screens.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nfccardtaptopayv101.R
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.BleAdvertisingState
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen3ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen3Screen(
    viewModel: Screen3ViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshPermissionsAndBluetooth()
        if (permissions.values.all { it }) {
            viewModel.startAdvertising()
        }
    }

    // Handle side effects like permission requests and starting advertising
    LaunchedEffect(
        uiState.hasRequiredPermissions,
        uiState.isBluetoothEnabled,
        uiState.isLoadingUserData,
        uiState.advertisingState
    ) {
        when {
            uiState.isLoadingUserData -> {
                // Wait until user data loaded before starting
            }
            !uiState.hasRequiredPermissions -> {
                permissionLauncher.launch(viewModel.getRequiredPermissions())
            }
            !uiState.isBluetoothEnabled -> {
                // Could prompt user to enable Bluetooth here
            }
            uiState.advertisingState is BleAdvertisingState.Idle -> {
                viewModel.startAdvertising()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAdvertising()
        }
    }

    // Show loading spinner while fetching user data
    if (uiState.isLoadingUserData) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Main UI when data is loaded
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Send Money",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!uiState.userProfileImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(uiState.userProfileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📷",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Name
            Text(
                text = uiState.userName.ifEmpty { "User" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Message
            Text(
                text = uiState.statusMessage,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = when (uiState.advertisingState) {
                    is BleAdvertisingState.Broadcasting -> Color(0xFF4CAF50)
                    is BleAdvertisingState.Error -> Color(0xFFE57373)
                    is BleAdvertisingState.Starting -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (uiState.advertisingState) {
                is BleAdvertisingState.Broadcasting -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {}
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Broadcasting Active",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                is BleAdvertisingState.Error -> {
                    val errorState = uiState.advertisingState as BleAdvertisingState.Error
                    Text(
                        text = "⚠️ ${errorState.message}",
                        fontSize = 14.sp,
                        color = Color(0xFFE57373),
                        textAlign = TextAlign.Center
                    )
                }
                is BleAdvertisingState.Starting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> { /* Idle or Stopped - no indicator */ }
            }
        }
    }
}

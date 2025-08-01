package com.example.nfccardtaptopayv101.ui.screens.ble

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.BleScanningState
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.NearbyUser
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen2ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen2Screen(
    viewModel: Screen2ViewModel,
    onUserSelected: (NearbyUser) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshPermissionsAndBluetooth()
        if (permissions.values.all { it }) {
            viewModel.startScanning()
        }
    }

    // Handle side effects like permission requests and starting scanning
    LaunchedEffect(
        uiState.hasRequiredPermissions,
        uiState.isBluetoothEnabled,
        uiState.scanningState
    ) {
        when {
            !uiState.hasRequiredPermissions -> {
                permissionLauncher.launch(viewModel.getRequiredPermissions())
            }
            !uiState.isBluetoothEnabled -> {
                // Could prompt user to enable Bluetooth here
            }
            uiState.scanningState is BleScanningState.Idle -> {
                viewModel.startScanning()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Request Money",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.stopScanning()
                            viewModel.startScanning()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh scan"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Section
            StatusSection(
                scanningState = uiState.scanningState,
                statusMessage = uiState.statusMessage,
                userCount = uiState.nearbyUsers.size,
                isScanning = uiState.isScanning
            )

            // Users List
            if (uiState.nearbyUsers.isEmpty() && uiState.scanningState is BleScanningState.Scanning) {
                EmptyStateSection()
            } else {
                NearbyUsersList(
                    users = uiState.nearbyUsers,
                    onUserTapped = { user ->
                        viewModel.onUserTapped(user)
                        onUserSelected(user)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusSection(
    scanningState: BleScanningState,
    statusMessage: String,
    userCount: Int,
    isScanning: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (scanningState) {
                is BleScanningState.Scanning -> MaterialTheme.colorScheme.primaryContainer
                is BleScanningState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (scanningState) {
                    is BleScanningState.Scanning -> {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scanning Active",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is BleScanningState.Starting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Starting...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is BleScanningState.Error -> {
                        Text(
                            text = "⚠️ Error",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {
                        Text(
                            text = "●",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status message
            Text(
                text = statusMessage,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = when (scanningState) {
                    is BleScanningState.Scanning -> MaterialTheme.colorScheme.onPrimaryContainer
                    is BleScanningState.Error -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            // User count
            if (userCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$userCount user${if (userCount == 1) "" else "s"} nearby",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (scanningState) {
                        is BleScanningState.Scanning -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateSection() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Looking for nearby users...",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Make sure other users have opened\nthe Send Money screen",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun NearbyUsersList(
    users: List<NearbyUser>,
    onUserTapped: (NearbyUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = users,
            key = { user -> user.userId }
        ) { user ->
            NearbyUserCard(
                user = user,
                onTapped = { onUserTapped(user) }
            )
        }
    }
}

@Composable
private fun NearbyUserCard(
    user: NearbyUser,
    onTapped: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300))
            .clickable { onTapped() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (user.isLoadingData) {
                    // Loading state
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                } else if (!user.userProfileImageUrl.isNullOrEmpty()) {
                    // Profile image
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user.userProfileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default avatar
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (user.isLoadingData) "Loading..." else user.userName.ifEmpty { "User ${user.userId}" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ID: ${user.userId}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Signal Strength & Distance Info
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Signal strength indicator
                SignalStrengthIndicator(rssi = user.rssi)

                Spacer(modifier = Modifier.height(4.dp))

                // Distance estimation
                Text(
                    text = getDistanceText(user.rssi),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Last seen
                Text(
                    text = getLastSeenText(user.lastSeen),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun SignalStrengthIndicator(rssi: Int) {
    val signalStrength = when {
        rssi >= -50 -> 4 // Excellent
        rssi >= -60 -> 3 // Good
        rssi >= -70 -> 2 // Fair
        rssi >= -80 -> 1 // Poor
        else -> 0 // Very Poor
    }

    val signalColor = when (signalStrength) {
        4 -> Color(0xFF4CAF50) // Green
        3 -> Color(0xFF8BC34A) // Light Green
        2 -> Color(0xFFFF9800) // Orange
        1 -> Color(0xFFFF5722) // Red Orange
        else -> Color(0xFFE57373) // Light Red
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(((index + 1) * 3).dp)
                    .background(
                        color = if (index < signalStrength) signalColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

private fun getDistanceText(rssi: Int): String {
    return when {
        rssi >= -50 -> "Very Close"
        rssi >= -60 -> "Close"
        rssi >= -70 -> "Nearby"
        rssi >= -80 -> "Far"
        else -> "Very Far"
    }
}

private fun getLastSeenText(lastSeen: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - lastSeen

    return when {
        diff < 5000 -> "Now"
        diff < 30000 -> "${diff / 1000}s ago"
        diff < 60000 -> "1m ago"
        else -> "${diff / 60000}m ago"
    }
}
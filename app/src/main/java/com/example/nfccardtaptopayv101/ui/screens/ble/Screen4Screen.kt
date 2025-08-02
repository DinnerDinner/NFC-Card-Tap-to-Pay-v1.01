package com.example.nfccardtaptopayv101.ui.screens.ble

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Clear
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
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.NearbyUser
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.PaymentRequestState
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen4ViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen4Screen(
    viewModel: Screen4ViewModel,
    selectedUser: NearbyUser,
    merchantUserId: String,
    onNavigateToWaitingScreen: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Set initial data when screen loads
    LaunchedEffect(selectedUser, merchantUserId) {
        viewModel.setSelectedUser(selectedUser)
        viewModel.setMerchantUserId(merchantUserId)
    }

    // Handle successful payment request
    LaunchedEffect(uiState.paymentRequestState) {
        if (uiState.paymentRequestState is PaymentRequestState.Success) {
            // Navigate to waiting screen after a short delay
            kotlinx.coroutines.delay(1500)
            onNavigateToWaitingScreen()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Request Payment",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
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
            // Top 20% - User Profile Section
            UserProfileSection(
                user = selectedUser,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
            )

            // Middle Section - Amount Display
            AmountDisplaySection(
                amount = uiState.currentAmount,
                formattedAmount = viewModel.getFormattedAmount(),
                isValid = uiState.isValidAmount,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
            )

            // Status Message
            if (uiState.statusMessage.isNotEmpty()) {
                StatusMessageSection(
                    message = uiState.statusMessage,
                    state = uiState.paymentRequestState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Bottom 60% - Number Keypad
            NumberKeypadSection(
                onNumberPressed = viewModel::onNumberPressed,
                onBackspacePressed = viewModel::onBackspacePressed,
                onClearPressed = viewModel::onClearPressed,
                onSendRequest = viewModel::sendPaymentRequest,
                isValidAmount = uiState.isValidAmount,
                isProcessing = uiState.paymentRequestState is PaymentRequestState.Processing,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun UserProfileSection(
    user: NearbyUser,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Profile Picture Circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!user.userProfileImageUrl.isNullOrEmpty()) {
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
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // User Name
        Text(
            text = user.userName.ifEmpty { "User ${user.userId}" },
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // User ID
        Text(
            text = "ID: ${user.userId}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AmountDisplaySection(
    amount: String,
    formattedAmount: String,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isValid) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "amount_color"
    )

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Amount to Request",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (amount == "0.00") "$0.00" else "$$amount",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusMessageSection(
    message: String,
    state: PaymentRequestState,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (state) {
        is PaymentRequestState.Processing -> MaterialTheme.colorScheme.primaryContainer
        is PaymentRequestState.Success -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        is PaymentRequestState.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (state) {
        is PaymentRequestState.Processing -> MaterialTheme.colorScheme.onPrimaryContainer
        is PaymentRequestState.Success -> Color(0xFF2E7D32)
        is PaymentRequestState.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state is PaymentRequestState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = message,
                fontSize = 14.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NumberKeypadSection(
    onNumberPressed: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onClearPressed: () -> Unit,
    onSendRequest: () -> Unit,
    isValidAmount: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Keypad Grid
        val keypadButtons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )

        keypadButtons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    KeypadButton(
                        text = key,
                        onClick = {
                            when (key) {
                                "⌫" -> onBackspacePressed()
                                else -> onNumberPressed(key)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clear Button
            OutlinedButton(
                onClick = onClearPressed,
                modifier = Modifier.weight(1f),
                enabled = !isProcessing
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }

            // Send Request Button
            Button(
                onClick = onSendRequest,
                modifier = Modifier.weight(2f),
                enabled = isValidAmount && !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending...")
                } else {
                    Text("Send Request", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (text == "⌫") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
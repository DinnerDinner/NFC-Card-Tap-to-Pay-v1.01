package com.example.nfccardtaptopayv101.ui.screens.ble

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.ModeSelectionUiState
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen1ViewModel

@Composable
fun Screen1Screen(
    onRequestMoney: () -> Unit,
    onSendMoney: () -> Unit,
    viewModel: Screen1ViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation
    LaunchedEffect(uiState) {
        when (uiState) {
            is ModeSelectionUiState.NavigateToRequestMoney -> {
                viewModel.clearNavigationFlag()
                onRequestMoney()
            }
            is ModeSelectionUiState.NavigateToSendMoney -> {
                viewModel.clearNavigationFlag()
                onSendMoney()
            }
            else -> { /* Do nothing */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Transaction Mode",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Request Money Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            onClick = {
                viewModel.onRequestMoneyClicked()
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CallReceived,
                        contentDescription = "Request Money",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Request Money",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Send Money Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            onClick = {
                viewModel.onSendMoneyClicked()
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Money",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Send Money",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
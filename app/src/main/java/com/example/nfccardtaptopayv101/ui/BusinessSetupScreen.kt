package com.example.nfccardtaptopayv101.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.BusinessSetupViewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.BusinessSetupUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(
    viewModel: BusinessSetupViewModel = viewModel()
) {
    var isBusiness by remember { mutableStateOf(false) }
    var businessName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Setup Your Business") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Are you a business owner?",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Yes / No", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = isBusiness,
                    onCheckedChange = { isBusiness = it }
                )
            }

            if (isBusiness) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business Name") },
                    leadingIcon = {
                        Icon(Icons.Filled.Business, contentDescription = "Business Icon")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isBusiness && businessName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter your business name.")
                        }
                        return@Button
                    }

                    if (!isBusiness) {
                        scope.launch {
                            snackbarHostState.showSnackbar("You chose 'No' for business. No setup needed.")
                        }
                        return@Button
                    }

                    viewModel.submitBusiness(businessName)

                },
                modifier = Modifier.align(Alignment.End),
                enabled = uiState !is BusinessSetupUiState.Loading
            ) {
                if (uiState is BusinessSetupUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...")
                } else {
                    Text("Submit")
                }
            }
        }
    }

    /** ---- UI‑STATE REACTIONS ---- */
    LaunchedEffect(uiState) {
        when (uiState) {
            is BusinessSetupUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as BusinessSetupUiState.Success).msg)
                businessName = ""
                isBusiness = false
                viewModel.reset()
            }
            is BusinessSetupUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as BusinessSetupUiState.Error).msg)
                viewModel.reset()
            }
            else -> {}
        }
    }
}

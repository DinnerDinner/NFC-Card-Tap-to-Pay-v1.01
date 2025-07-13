package com.example.nfccardtaptopayv101.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.BusinessUiState
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.BusinessViewModel
import com.example.nfccardtaptopayv101.ui.navigation.MposNavGraph
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessUnifiedScreen(
    viewModel: BusinessViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf("") }

    // This controls which screen is shown inside this composable
    var currentScreen by remember { mutableStateOf("business") }

    // Refresh business status every time this screen enters composition
    LaunchedEffect(Unit) {
        viewModel.checkBusinessExists()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreen) {
                "business" -> {
                    when (state) {
                        is BusinessUiState.Loading -> {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is BusinessUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Something went wrong")
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { viewModel.checkBusinessExists() }) {
                                    Text("Retry")
                                }
                            }
                        }

                        is BusinessUiState.ShowSetupForm -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Start your journey",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(24.dp))

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Business Name") },
                                    leadingIcon = { Icon(Icons.Filled.Business, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        if (nameInput.isBlank()) {
                                            scope.launch {
                                                snackbar.showSnackbar("Enter a business name")
                                            }
                                        } else {
                                            viewModel.submitBusiness(nameInput)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Let's Go!")
                                }
                            }
                        }

                        is BusinessUiState.ShowDashboard -> {
                            val name = (state as BusinessUiState.ShowDashboard).businessName
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    name.ifBlank { "Your Business" },
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Welcome to your Business",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(32.dp))

                                Button(onClick = { /* TODO: Navigate to Sales */ }) {
                                    Text("Sales Page")
                                }
                                Spacer(Modifier.height(16.dp))
                                // THIS BUTTON CHANGES SCREEN STATE TO PRODUCT MANAGER
                                Button(onClick = { currentScreen = "product_manager" }) {
                                    Text("Product Manager")
                                }
                            }
                        }

                        else -> {}
                    }
                }
                "product_manager" -> {
                    // **Launch isolated mPOS NavGraph here**
                    MposNavGraph(
                        onBackToDashboard = { currentScreen = "business" }
                    )
                }
            }
        }
    }
}

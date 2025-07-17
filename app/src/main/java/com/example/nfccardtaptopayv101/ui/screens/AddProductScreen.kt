package com.example.nfccardtaptopayv101.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    vm: AddProductViewModel = viewModel(),
    managerVM: ProductManagerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val snackbar = remember { SnackbarHostState() }
    var rawKeywords by remember { mutableStateOf("") }
    val uiState by vm.ui.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.LightGray, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { /* TODO: Upload image */ }) {
                    Text("Upload Image")
                }
            }

            val title by vm.title.collectAsState()

            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { newText ->
                        if (newText.length <= 52) {
                            vm.title.value = newText  // keep updating ViewModel
                        }
                    },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${title.length} / 52",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End),
                    color = if (title.length >= 52) MaterialTheme.colorScheme.error else Color.Gray
                )
            }




            OutlinedTextField(
                value = vm.price.collectAsState().value,
                onValueChange = { vm.price.value = it },
                label = { Text("Price") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.sku.collectAsState().value,
                onValueChange = { vm.sku.value = it },
                label = { Text("SKU (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vm.description.collectAsState().value,
                onValueChange = { vm.description.value = it },
                label = { Text("Description (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            OutlinedTextField(
                value = rawKeywords,
                onValueChange = {
                    rawKeywords = it
                    vm.updateKeywordsFromRaw(it)
                },
                label = { Text("Keywords (comma‑separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.keywords.collectAsState().value.forEach {
                    AssistChip(label = { Text(it) }, onClick = {})
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = { vm.submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = uiState !is AddProductUiState.Submitting
            ) {
                if (uiState is AddProductUiState.Submitting)
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                else
                    Text("Add Product")
            }
        }
    }

    // ✅ Redirect + reload
    LaunchedEffect(uiState) {
        when (uiState) {
            is AddProductUiState.Success -> {
//                snackbar.showSnackbar("✅ Product added")
                managerVM.refreshProducts()        // reload full list
                delay(300)                         // smooth transition
                vm.reset()
                onBack()

            }

            is AddProductUiState.Error -> {
                snackbar.showSnackbar((uiState as AddProductUiState.Error).msg)
            }

            else -> {}
        }
    }
}


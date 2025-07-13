package com.example.nfccardtaptopayv101.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagerScreen(
    vm: ProductManagerViewModel = viewModel(),
    onBack: () -> Unit,
    onAddProduct: () -> Unit
) {
    val state by vm.reversedProducts.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Manager") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            Button(
                onClick = onAddProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Add Product")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state) { product ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(0.5f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Business,
                                        contentDescription = "Product image",
                                        modifier = Modifier.fillMaxSize(0.8f)
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(0.5f)
                                        .fillMaxHeight()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = product.title,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "$${"%.2f".format(product.price)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Text(
                                            text = "SKU: ${product.sku ?: "N/A"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}








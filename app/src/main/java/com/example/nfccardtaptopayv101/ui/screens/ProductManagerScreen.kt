package com.example.nfccardtaptopayv101.ui.screens
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.*
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
    onBack: () -> Unit          // allow dashboard to pop back
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.loadProducts() }

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

        when (state) {
            ProductMgrState.Loading -> Box(
                Modifier.fillMaxSize().padding(pad),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is ProductMgrState.Error -> Box(
                Modifier.fillMaxSize().padding(pad),
                Alignment.Center
            ) { Text((state as ProductMgrState.Error).msg) }

            is ProductMgrState.Ready -> {
                val products = (state as ProductMgrState.Ready).products
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    products.forEach {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(it.title, style = MaterialTheme.typography.titleMedium)
                                    Text("$${it.price}", style = MaterialTheme.typography.bodyMedium)
                                }
                                /* TODO: edit / delete buttons */
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { /* TODO open AddProductScreen */ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Product") }
                }
            }
        }
    }
}

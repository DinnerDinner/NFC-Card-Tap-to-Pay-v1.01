package com.example.nfccardtaptopayv101.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ProductManagerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagerScreen(
    vm: ProductManagerViewModel = viewModel(),
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    val visibleProducts by vm.visibleProducts.collectAsState()
    val allKeywords by vm.allFilters.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val selectedKeyword by vm.selectedFilter.collectAsState()

    val scaffoldState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(240.dp)) {
                Text(
                    "Filter by Keyword",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    allKeywords.forEach { keyword ->
                        Text(
                            text = keyword,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newFilter = if (selectedKeyword == keyword) null else keyword
                                    vm.selectFilter(newFilter)
                                    coroutineScope.launch { scaffoldState.close() }
                                }
                                .padding(12.dp),
                            color = if (selectedKeyword == keyword)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        drawerState = scaffoldState,
        gesturesEnabled = scaffoldState.isOpen
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Product Manager") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                if (scaffoldState.isClosed) scaffoldState.open()
                                else scaffoldState.close()
                            }
                        }) {
                            Text("Filter")
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = vm::updateSearchQuery,
                    label = { Text("Search products") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                Button(
                    onClick = onAddProduct,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Text("Add Product")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (visibleProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No products match your search.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(visibleProducts) { product ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clickable {
                                        product.sku?.let { onEditProduct(it) }
                                    }
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

    // Refresh once on launch
    LaunchedEffect(Unit) {
        vm.refreshProducts()
    }
}



















//package com.example.nfccardtaptopayv101.ui.screens
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Business
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ProductManagerViewModel
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProductManagerScreen(
//    vm: ProductManagerViewModel = viewModel(),
//    onBack: () -> Unit,
//    onAddProduct: () -> Unit,
//    onEditProduct: (String) -> Unit
//) {
//    val allProducts by vm.reversedProducts.collectAsState()
//    val scaffoldState = rememberDrawerState(DrawerValue.Closed)
//    val coroutineScope = rememberCoroutineScope()
//
//    var selectedKeyword by remember { mutableStateOf<String?>(null) }
//
//    val filteredProducts = remember(allProducts, selectedKeyword) {
//        if (selectedKeyword.isNullOrEmpty()) allProducts
//        else allProducts.filter { product ->
//            product.keywords?.split(",")?.map { it.trim() }?.contains(selectedKeyword) == true
//        }
//    }
//
//    val allKeywords = remember(allProducts) {
//        allProducts.flatMap {
//            it.keywords?.split(",")?.map { k -> k.trim() } ?: emptyList()
//        }.toSet()
//    }
//
//    ModalNavigationDrawer(
//        drawerContent = {
//            ModalDrawerSheet(modifier = Modifier.width(240.dp)) {
//                Text(
//                    "Filter by Keyword",
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier.padding(16.dp)
//                )
//                Divider()
//                // **Make this scrollable vertically**
//                Column(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .verticalScroll(rememberScrollState())
//                ) {
//                    allKeywords.forEach { keyword ->
//                        Text(
//                            text = keyword,
//                            style = MaterialTheme.typography.bodyMedium,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .clickable {
//                                    selectedKeyword = if (selectedKeyword == keyword) null else keyword
//                                    coroutineScope.launch { scaffoldState.close() }
//                                }
//                                .padding(12.dp),
//                            color = if (selectedKeyword == keyword)
//                                MaterialTheme.colorScheme.primary
//                            else
//                                MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//                }
//            }
//        },
//        drawerState = scaffoldState,
//        gesturesEnabled = scaffoldState.isOpen
//    ) {
//        Scaffold(
//            topBar = {
//                TopAppBar(
//                    title = { Text("Product Manager") },
//                    navigationIcon = {
//                        IconButton(onClick = onBack) {
//                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                        }
//                    },
//                    actions = {
//                        TextButton(onClick = {
//                            coroutineScope.launch {
//                                if (scaffoldState.isClosed) scaffoldState.open()
//                                else scaffoldState.close()
//                            }
//                        }) {
//                            Text("Filter")
//                        }
//                    }
//                )
//            }
//        ) { pad ->
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(pad)
//            ) {
//                Button(
//                    onClick = onAddProduct,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp, vertical = 14.dp)
//                ) {
//                    Text("Add Product")
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                if (allProducts.isEmpty()) {
//                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        CircularProgressIndicator()
//                    }
//                } else {
//                    LazyColumn(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.spacedBy(12.dp),
//                        contentPadding = PaddingValues(bottom = 24.dp)
//                    ) {
//                        items(filteredProducts) { product ->
//                            ElevatedCard(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(180.dp)
//                                    .clickable {
//                                        product.sku?.let { onEditProduct(it) }
//                                    }
//                            ) {
//                                Row(modifier = Modifier.fillMaxSize()) {
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxHeight()
//                                            .weight(0.5f),
//                                        contentAlignment = Alignment.Center
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Filled.Business,
//                                            contentDescription = "Product image",
//                                            modifier = Modifier.fillMaxSize(0.8f)
//                                        )
//                                    }
//                                    Column(
//                                        modifier = Modifier
//                                            .weight(0.5f)
//                                            .fillMaxHeight()
//                                            .padding(12.dp),
//                                        verticalArrangement = Arrangement.SpaceBetween
//                                    ) {
//                                        Column {
//                                            Text(
//                                                text = product.title,
//                                                style = MaterialTheme.typography.titleLarge
//                                            )
//                                            Spacer(modifier = Modifier.height(6.dp))
//                                            Text(
//                                                text = "$${"%.2f".format(product.price)}",
//                                                style = MaterialTheme.typography.titleMedium,
//                                                color = MaterialTheme.colorScheme.primary
//                                            )
//                                        }
//                                        Box(
//                                            modifier = Modifier.fillMaxWidth(),
//                                            contentAlignment = Alignment.BottomEnd
//                                        ) {
//                                            Text(
//                                                text = "SKU: ${product.sku ?: "N/A"}",
//                                                style = MaterialTheme.typography.labelMedium,
//                                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // 🔄 Refresh on first load
//    LaunchedEffect(Unit) {
//        vm.refreshProducts()
//    }
//}

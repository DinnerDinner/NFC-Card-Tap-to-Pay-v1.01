package com.example.nfccardtaptopayv101.ui.screens
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SalesPageScreen(
    vm: SalesPageViewModel,
    onBack: () -> Unit,
    onCheckoutClicked: () -> Unit,
    onScanClicked: () -> Unit
) {
    val state by vm.state.collectAsState()
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
                    title = { Text("Sales Page") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onScanClicked) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                        }
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
            },
            bottomBar = {
                val localState = state
                val enabled = localState is com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageState.Ready &&
                        localState.totalItems > 0

                BottomAppBar(
                    tonalElevation = 8.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = onCheckoutClicked,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        Spacer(Modifier.width(8.dp))
                        Text("Checkout (${(localState as? com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageState.Ready)?.totalItems ?: 0} items)")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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

                when (val localState = state) {
                    is com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    is com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageState.Error -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text("Error: ${localState.msg}") }

                    is com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageState.Ready -> {
                        if (visibleProducts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No products match your search.", style = MaterialTheme.typography.bodyLarge)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(visibleProducts) { product ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Start
                                        ) {

                                            Box(
                                                modifier = Modifier
                                                    .width(140.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        color = Color.LightGray,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val imageUrl = product.image_url

                                                if (!imageUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(imageUrl)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = product.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        "IMG",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(16.dp))

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = product.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 18.sp,
                                                        color = Color.Black,
                                                        maxLines = 4
                                                    )
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(
                                                        text = "$${"%.2f".format(product.price)}",
                                                        fontSize = 16.sp,
                                                        color = Color.Gray
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Start
                                                ) {
                                                    IconButton(
                                                        onClick = { vm.decrementQuantity(product.id) },
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.Black)
                                                    }

                                                    Text(
                                                        text = (localState.quantities[product.id] ?: 0).toString(),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black,
                                                        modifier = Modifier
                                                            .padding(horizontal = 8.dp)
                                                            .width(32.dp),
                                                        textAlign = TextAlign.Center
                                                    )

                                                    IconButton(
                                                        onClick = { vm.incrementQuantity(product.id) },
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.Black)
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
            }
        }
    }
}















//package com.example.nfccardtaptopayv101.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageState
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SalesPageScreen(
//    vm: SalesPageViewModel,  // MUST pass shared VM instance from parent, do NOT call viewModel() here
//    onBack: () -> Unit,
//    onCheckoutClicked: () -> Unit,
//    onScanClicked: () -> Unit
//) {
//    val state = vm.state.collectAsState().value
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Sales Page") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                actions = {
//                    IconButton(onClick = onScanClicked) {
//                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan (placeholder)")
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            val enabled = (state as? SalesPageState.Ready)?.totalItems ?: 0 > 0
//
//            BottomAppBar(
//                tonalElevation = 8.dp,
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
//            ) {
//                Button(
//                    onClick = onCheckoutClicked,
//                    enabled = enabled,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
//                    Spacer(Modifier.width(8.dp))
//                    Text("Checkout (${(state as? SalesPageState.Ready)?.totalItems ?: 0} items)")
//                }
//            }
//        }
//    ) { padding ->
//
//        when (state) {
//            is SalesPageState.Loading -> Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator()
//            }
//
//            is SalesPageState.Error -> Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Error: ${state.msg}")
//            }
//
//            is SalesPageState.Ready -> {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(padding)
//                        .padding(horizontal = 16.dp, vertical = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    items(state.products) { product ->
//
//                        Card(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(180.dp),
//                            shape = RoundedCornerShape(16.dp),
//                            colors = CardDefaults.cardColors(containerColor = Color.White)
//                        ) {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                                    .padding(12.dp),
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.Start
//                            ) {
//                                // Image Box
//                                Box(
//                                    modifier = Modifier
//                                        .width(140.dp)
//                                        .fillMaxHeight()
//                                        .background(
//                                            color = Color.LightGray,
//                                            shape = RoundedCornerShape(10.dp)
//                                        ),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    Text(
//                                        "IMG",
//                                        style = MaterialTheme.typography.labelLarge,
//                                        color = Color.DarkGray
//                                    )
//                                }
//
//                                Spacer(Modifier.width(16.dp))
//
//                                // Right side Column: Title+Price on top, Stepper below
//                                Column(
//                                    modifier = Modifier
//                                        .weight(1f)
//                                        .fillMaxHeight(),
//                                    verticalArrangement = Arrangement.SpaceBetween
//                                ) {
//                                    // Title and Price on top
//                                    Column {
//                                        Text(
//                                            text = product.title,
//                                            fontWeight = FontWeight.Bold,
//                                            fontSize = 18.sp,
//                                            color = Color.Black,
//                                            maxLines = 4
//                                        )
//                                        Spacer(Modifier.height(6.dp))
//                                        Text(
//                                            text = "$${"%.2f".format(product.price)}",
//                                            fontSize = 16.sp,
//                                            color = Color.Gray
//                                        )
//                                    }
//
//                                    // Stepper below
//                                    Row(
//                                        modifier = Modifier.fillMaxWidth(),
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        horizontalArrangement = Arrangement.Start
//                                    ) {
//                                        IconButton(
//                                            onClick = { vm.decrementQuantity(product.id) },
//                                            modifier = Modifier.size(40.dp)
//                                        ) {
//                                            Icon(
//                                                imageVector = Icons.Default.Remove,
//                                                contentDescription = "Decrease",
//                                                tint = Color.Black
//                                            )
//                                        }
//
//                                        Text(
//                                            text = (state.quantities[product.id] ?: 0).toString(),
//                                            fontSize = 18.sp,
//                                            fontWeight = FontWeight.Bold,
//                                            color = Color.Black,
//                                            modifier = Modifier
//                                                .padding(horizontal = 8.dp)
//                                                .width(32.dp),
//                                            textAlign = TextAlign.Center
//                                        )
//
//                                        IconButton(
//                                            onClick = { vm.incrementQuantity(product.id) },
//                                            modifier = Modifier.size(40.dp)
//                                        ) {
//                                            Icon(
//                                                imageVector = Icons.Default.Add,
//                                                contentDescription = "Increase",
//                                                tint = Color.Black
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
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//

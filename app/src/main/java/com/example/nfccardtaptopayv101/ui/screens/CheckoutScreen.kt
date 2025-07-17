package com.example.nfccardtaptopayv101.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.nfccardtaptopayv101.CheckoutTapPaymentActivity
import com.example.nfccardtaptopayv101.ui.navigation.MposScreens
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    vm: SalesPageViewModel = viewModel(),
    navController: NavHostController,
    onBack: () -> Unit
) {
    val uiState by vm.state.collectAsState()
    val selectedItems = remember(uiState) { vm.getSelectedProducts() }
    val grandTotal = remember(uiState) { vm.getGrandTotal() }
    val context = LocalContext.current
    val activity = context as? Activity

    // Launcher for CheckoutTapPaymentActivity result
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // On successful payment, clear cart and navigate to SalesPage
            vm.clearCart()
            navController.navigate(MposScreens.SalesPage.route) {
                popUpTo(MposScreens.ProductManager.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                tonalElevation = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.height(120.dp) // doubled from default ~56dp to 120dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { vm.clearCart() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Reset Cart",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "Total: $${"%.2f".format(grandTotal)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (selectedItems.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, CheckoutTapPaymentActivity::class.java).apply {
                                        putExtra("amount_to_charge", grandTotal.toFloat())
                                    }
                                    activity?.let { launcher.launch(intent) }
                                },
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp) // bigger button height
                            ) {
                                Text(
                                    "Pay Now",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }


    ) { padding ->

        if (selectedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No items in cart")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(selectedItems) { (product, qty) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
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
                                Text(
                                    "IMG",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.DarkGray
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = product.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black,
                                    maxLines = 3
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Unit: $${"%.2f".format(product.price)}",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "SKU: ${product.sku ?: "N/A"}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "x$qty",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "$${"%.2f".format(product.price * qty)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.nfccardtaptopayv101.CheckoutTapPaymentActivity
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.Product
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
//import android.content.Intent
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CheckoutScreen(
//    vm: SalesPageViewModel = viewModel(),
//    onBack: () -> Unit
//) {
//    val uiState by vm.state.collectAsState()
//    val selectedItems = remember(uiState) { vm.getSelectedProducts() }
//    val grandTotal = remember(uiState) { vm.getGrandTotal() }
//    val context = LocalContext.current
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Checkout") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar(
//                tonalElevation = 8.dp,
//                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    IconButton(
//                        onClick = { vm.clearCart() },
//                        modifier = Modifier.size(36.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Delete,
//                            contentDescription = "Reset Cart",
//                            tint = MaterialTheme.colorScheme.error
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.weight(1f))
//
//                    Column(horizontalAlignment = Alignment.End) {
//                        Text(
//                            "Total: $${"%.2f".format(grandTotal)}",
//                            style = MaterialTheme.typography.titleLarge,
//                            fontWeight = FontWeight.Bold
//                        )
//
//                        if (selectedItems.isNotEmpty()) {
//                            Spacer(modifier = Modifier.height(8.dp))
//                            Button(
//                                onClick = {
//                                    // Launch NFC Tap Payment activity with total amount
//                                    context.startActivity(
//                                        Intent(context, CheckoutTapPaymentActivity::class.java).apply {
//                                            putExtra("amount_to_charge", grandTotal.toFloat())
//                                        }
//                                    )
//                                },
//                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
//                                shape = RoundedCornerShape(12.dp)
//                            ) {
//                                Text("Pay Now")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    ) { padding ->
//
//        if (selectedItems.isEmpty()) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("No items in cart")
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(padding)
//                    .padding(horizontal = 16.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(selectedItems) { (product, qty) ->
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(150.dp),
//                        shape = RoundedCornerShape(16.dp),
//                        colors = CardDefaults.cardColors(containerColor = Color.White)
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .padding(12.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.Start
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .width(140.dp)
//                                    .fillMaxHeight()
//                                    .background(
//                                        color = Color.LightGray,
//                                        shape = RoundedCornerShape(10.dp)
//                                    ),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(
//                                    "IMG",
//                                    style = MaterialTheme.typography.labelLarge,
//                                    color = Color.DarkGray
//                                )
//                            }
//
//                            Spacer(Modifier.width(16.dp))
//
//                            Column(
//                                modifier = Modifier.weight(1f),
//                                verticalArrangement = Arrangement.Center
//                            ) {
//                                Text(
//                                    text = product.title,
//                                    fontWeight = FontWeight.Bold,
//                                    fontSize = 18.sp,
//                                    color = Color.Black,
//                                    maxLines = 3
//                                )
//                                Spacer(Modifier.height(4.dp))
//                                Text(
//                                    text = "Unit: $${"%.2f".format(product.price)}",
//                                    fontSize = 14.sp,
//                                    color = Color.Gray
//                                )
//                                Spacer(Modifier.height(4.dp))
//                                Text(
//                                    text = "SKU: ${product.sku ?: "N/A"}",
//                                    fontSize = 13.sp,
//                                    color = Color.Gray
//                                )
//                            }
//
//                            Column(
//                                horizontalAlignment = Alignment.End,
//                                verticalArrangement = Arrangement.Center
//                            ) {
//                                Text(
//                                    "x$qty",
//                                    fontSize = 18.sp,
//                                    fontWeight = FontWeight.Bold,
//                                    color = Color.Black
//                                )
//                                Spacer(Modifier.height(6.dp))
//                                Text(
//                                    "$${"%.2f".format(product.price * qty)}",
//                                    fontSize = 16.sp,
//                                    fontWeight = FontWeight.SemiBold,
//                                    color = MaterialTheme.colorScheme.primary
//                                )
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
////package com.example.nfccardtaptopayv101.ui.screens
////
////import androidx.compose.foundation.background
////import androidx.compose.foundation.layout.*
////import androidx.compose.foundation.lazy.LazyColumn
////import androidx.compose.foundation.lazy.items
////import androidx.compose.foundation.shape.RoundedCornerShape
////import androidx.compose.material.icons.Icons
////import androidx.compose.material.icons.filled.ArrowBack
////import androidx.compose.material.icons.filled.Delete
////import androidx.compose.material3.*
////import androidx.compose.runtime.*
////import androidx.compose.ui.Alignment
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.graphics.Color
////import androidx.compose.ui.text.font.FontWeight
////import androidx.compose.ui.text.style.TextAlign
////import androidx.compose.ui.unit.dp
////import androidx.compose.ui.unit.sp
////import androidx.lifecycle.viewmodel.compose.viewModel
////import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.Product
////import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
////
////@OptIn(ExperimentalMaterial3Api::class)
////@Composable
////fun CheckoutScreen(
////    vm: SalesPageViewModel = viewModel(),
////    onBack: () -> Unit,
////    onPayNow: (Float) -> Unit
////) {
////    val uiState by vm.state.collectAsState()
////    val selectedItems = remember(uiState) { vm.getSelectedProducts() }
////    val grandTotal = remember(uiState) { vm.getGrandTotal() }
////
////    Scaffold(
////        topBar = {
////            TopAppBar(
////                title = { Text("Checkout") },
////                navigationIcon = {
////                    IconButton(onClick = onBack) {
////                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
////                    }
////                }
////            )
////        },
////        bottomBar = {
////            BottomAppBar(
////                tonalElevation = 8.dp,
////                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
////            ) {
////                Row(
////                    modifier = Modifier.fillMaxWidth(),
////                    verticalAlignment = Alignment.CenterVertically
////                ) {
////                    IconButton(
////                        onClick = { vm.clearCart() },
////                        modifier = Modifier.size(36.dp)
////                    ) {
////                        Icon(
////                            imageVector = Icons.Default.Delete,
////                            contentDescription = "Reset Cart",
////                            tint = MaterialTheme.colorScheme.error
////                        )
////                    }
////
////                    Spacer(modifier = Modifier.weight(1f))
////
////                    Column(horizontalAlignment = Alignment.End) {
////                        Text(
////                            "Total: $${"%.2f".format(grandTotal)}",
////                            style = MaterialTheme.typography.titleLarge,
////                            fontWeight = FontWeight.Bold
////                        )
////
////                        // Add "Pay Now" button only when items are in cart
////                        if (selectedItems.isNotEmpty()) {
////                            Spacer(modifier = Modifier.height(8.dp))
////                            Button(
////                                onClick = { onPayNow(grandTotal.toFloat()) },
////                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
////                                shape = RoundedCornerShape(12.dp)
////                            ) {
////                                Text("Pay Now")
////                            }
////                        }
////                    }
////                }
////            }
////        }
////    ) { padding ->
////
////        if (selectedItems.isEmpty()) {
////            Box(
////                modifier = Modifier
////                    .fillMaxSize()
////                    .padding(padding),
////                contentAlignment = Alignment.Center
////            ) {
////                Text("No items in cart")
////            }
////        } else {
////            LazyColumn(
////                modifier = Modifier
////                    .fillMaxSize()
////                    .padding(padding)
////                    .padding(horizontal = 16.dp, vertical = 8.dp),
////                verticalArrangement = Arrangement.spacedBy(12.dp)
////            ) {
////                items(selectedItems) { (product, qty) ->
////                    Card(
////                        modifier = Modifier
////                            .fillMaxWidth()
////                            .height(150.dp),
////                        shape = RoundedCornerShape(16.dp),
////                        colors = CardDefaults.cardColors(containerColor = Color.White)
////                    ) {
////                        Row(
////                            modifier = Modifier
////                                .fillMaxSize()
////                                .padding(12.dp),
////                            verticalAlignment = Alignment.CenterVertically,
////                            horizontalArrangement = Arrangement.Start
////                        ) {
////                            Box(
////                                modifier = Modifier
////                                    .width(140.dp)
////                                    .fillMaxHeight()
////                                    .background(
////                                        color = Color.LightGray,
////                                        shape = RoundedCornerShape(10.dp)
////                                    ),
////                                contentAlignment = Alignment.Center
////                            ) {
////                                Text(
////                                    "IMG",
////                                    style = MaterialTheme.typography.labelLarge,
////                                    color = Color.DarkGray
////                                )
////                            }
////
////                            Spacer(Modifier.width(16.dp))
////
////                            Column(
////                                modifier = Modifier.weight(1f),
////                                verticalArrangement = Arrangement.Center
////                            ) {
////                                Text(
////                                    text = product.title,
////                                    fontWeight = FontWeight.Bold,
////                                    fontSize = 18.sp,
////                                    color = Color.Black,
////                                    maxLines = 3
////                                )
////                                Spacer(Modifier.height(4.dp))
////                                Text(
////                                    text = "Unit: $${"%.2f".format(product.price)}",
////                                    fontSize = 14.sp,
////                                    color = Color.Gray
////                                )
////                                Spacer(Modifier.height(4.dp))
////                                Text(
////                                    text = "SKU: ${product.sku ?: "N/A"}",
////                                    fontSize = 13.sp,
////                                    color = Color.Gray
////                                )
////                            }
////
////                            Column(
////                                horizontalAlignment = Alignment.End,
////                                verticalArrangement = Arrangement.Center
////                            ) {
////                                Text(
////                                    "x$qty",
////                                    fontSize = 18.sp,
////                                    fontWeight = FontWeight.Bold,
////                                    color = Color.Black
////                                )
////                                Spacer(Modifier.height(6.dp))
////                                Text(
////                                    "$${"%.2f".format(product.price * qty)}",
////                                    fontSize = 16.sp,
////                                    fontWeight = FontWeight.SemiBold,
////                                    color = MaterialTheme.colorScheme.primary
////                                )
////                            }
////                        }
////                    }
////                }
////            }
////        }
////    }
////}
////
////
////
////
//////package com.example.nfccardtaptopayv101.ui.screens
//////
//////import androidx.compose.foundation.background
//////import androidx.compose.foundation.layout.*
//////import androidx.compose.foundation.lazy.LazyColumn
//////import androidx.compose.foundation.lazy.items
//////import androidx.compose.foundation.shape.RoundedCornerShape
//////import androidx.compose.material.icons.Icons
//////import androidx.compose.material.icons.filled.ArrowBack
//////import androidx.compose.material.icons.filled.Delete
//////import androidx.compose.material3.*
//////import androidx.compose.runtime.*
//////import androidx.compose.ui.Alignment
//////import androidx.compose.ui.Modifier
//////import androidx.compose.ui.graphics.Color
//////import androidx.compose.ui.text.font.FontWeight
//////import androidx.compose.ui.text.style.TextAlign
//////import androidx.compose.ui.unit.dp
//////import androidx.compose.ui.unit.sp
//////import androidx.lifecycle.viewmodel.compose.viewModel
//////import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.Product
//////import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel
//////
//////@OptIn(ExperimentalMaterial3Api::class)
//////@Composable
//////fun CheckoutScreen(
//////    vm: SalesPageViewModel = viewModel(),   // **shared** ViewModel
//////    onBack: () -> Unit
//////) {
//////
//////    /* --------------------------------------------------------------------
//////       Pull everything you need from the SAME ViewModel that SalesPage uses
//////    -------------------------------------------------------------------- */
//////    val uiState by vm.state.collectAsState()
//////
//////    // Derive checkout‑specific data whenever state changes
//////    val selectedItems = remember(uiState) { vm.getSelectedProducts() }
//////    val grandTotal   = remember(uiState) { vm.getGrandTotal() }
//////
//////    Scaffold(
//////        topBar = {
//////            TopAppBar(
//////                title = { Text("Checkout") },
//////                navigationIcon = {
//////                    IconButton(onClick = onBack) {
//////                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//////                    }
//////                }
//////            )
//////        },
//////        bottomBar = {
//////            BottomAppBar(
//////                tonalElevation = 8.dp,
//////                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
//////            ) {
//////                Row(
//////                    modifier = Modifier.fillMaxWidth(),
//////                    verticalAlignment = Alignment.CenterVertically
//////                ) {
//////                    IconButton(
//////                        onClick = { vm.clearCart() },
//////                        modifier = Modifier.size(36.dp)
//////                    ) {
//////                        Icon(
//////                            imageVector = Icons.Default.Delete, // trash bin icon
//////                            contentDescription = "Reset Cart",
//////                            tint = MaterialTheme.colorScheme.error
//////                        )
//////                    }
//////
//////                    Spacer(modifier = Modifier.weight(1f))
//////
//////                    Text(
//////                        "Total: $${"%.2f".format(grandTotal)}",
//////                        style = MaterialTheme.typography.titleLarge,
//////                        fontWeight = FontWeight.Bold,
//////                        textAlign = TextAlign.End
//////                    )
//////                }
//////            }
//////        }
//////    ) { padding ->
//////
//////        if (selectedItems.isEmpty()) {
//////            /* -------------------------------------------
//////               Nothing selected (edge‑case, shouldn’t happen
//////               if you disabled Checkout button correctly)
//////            ------------------------------------------- */
//////            Box(
//////                modifier = Modifier
//////                    .fillMaxSize()
//////                    .padding(padding),
//////                contentAlignment = Alignment.Center
//////            ) {
//////                Text("No items in cart")
//////            }
//////        } else {
//////            /* -------------------------
//////               List of selected products
//////            ------------------------- */
//////            LazyColumn(
//////                modifier = Modifier
//////                    .fillMaxSize()
//////                    .padding(padding)
//////                    .padding(horizontal = 16.dp, vertical = 8.dp),
//////                verticalArrangement = Arrangement.spacedBy(12.dp)
//////            ) {
//////                items(selectedItems) { (product, qty) ->
//////
//////                    Card(
//////                        modifier = Modifier
//////                            .fillMaxWidth()
//////                            .height(150.dp),
//////                        shape = RoundedCornerShape(16.dp),
//////                        colors = CardDefaults.cardColors(containerColor = Color.White)
//////                    ) {
//////                        Row(
//////                            modifier = Modifier
//////                                .fillMaxSize()
//////                                .padding(12.dp),
//////                            verticalAlignment = Alignment.CenterVertically,
//////                            horizontalArrangement = Arrangement.Start
//////                        ) {
//////                            /*  --- Image placeholder (same style as SalesPage) --- */
//////                            Box(
//////                                modifier = Modifier
//////                                    .width(140.dp)
//////                                    .fillMaxHeight()
//////                                    .background(
//////                                        color = Color.LightGray,
//////                                        shape = RoundedCornerShape(10.dp)
//////                                    ),
//////                                contentAlignment = Alignment.Center
//////                            ) {
//////                                Text(
//////                                    "IMG",
//////                                    style = MaterialTheme.typography.labelLarge,
//////                                    color = Color.DarkGray
//////                                )
//////                            }
//////
//////                            Spacer(Modifier.width(16.dp))
//////
//////                            /*  --- Product info + per‑item total & qty  --- */
//////                            Column(
//////                                modifier = Modifier.weight(1f),
//////                                verticalArrangement = Arrangement.Center
//////                            ) {
//////                                Text(
//////                                    text = product.title,
//////                                    fontWeight = FontWeight.Bold,
//////                                    fontSize = 18.sp,
//////                                    color = Color.Black,
//////                                    maxLines = 3
//////                                )
//////                                Spacer(Modifier.height(4.dp))
//////                                Text(
//////                                    text = "Unit: $${"%.2f".format(product.price)}",
//////                                    fontSize = 14.sp,
//////                                    color = Color.Gray
//////                                )
//////                                Spacer(Modifier.height(4.dp))
//////                                Text(
//////                                    text = "SKU: ${product.sku ?: "N/A"}",
//////                                    fontSize = 13.sp,
//////                                    color = Color.Gray
//////                                )
//////                            }
//////
//////                            /*  --- Qty & line total on the right --- */
//////                            Column(
//////                                horizontalAlignment = Alignment.End,
//////                                verticalArrangement = Arrangement.Center
//////                            ) {
//////                                Text(
//////                                    "x$qty",
//////                                    fontSize = 18.sp,
//////                                    fontWeight = FontWeight.Bold,
//////                                    color = Color.Black
//////                                )
//////                                Spacer(Modifier.height(6.dp))
//////                                Text(
//////                                    "$${"%.2f".format(product.price * qty)}",
//////                                    fontSize = 16.sp,
//////                                    fontWeight = FontWeight.SemiBold,
//////                                    color = MaterialTheme.colorScheme.primary
//////                                )
//////                            }
//////                        }
//////                    }
//////                }
//////            }
//////        }
//////    }
//////}
////////
package com.example.nfccardtaptopayv101.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.Product
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.SalesPageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    vm: SalesPageViewModel = viewModel(),   // **shared** ViewModel
    onBack: () -> Unit
) {

    /* --------------------------------------------------------------------
       Pull everything you need from the SAME ViewModel that SalesPage uses
    -------------------------------------------------------------------- */
    val uiState by vm.state.collectAsState()

    // Derive checkout‑specific data whenever state changes
    val selectedItems = remember(uiState) { vm.getSelectedProducts() }
    val grandTotal   = remember(uiState) { vm.getGrandTotal() }

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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
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
                            imageVector = Icons.Default.Delete, // trash bin icon
                            contentDescription = "Reset Cart",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        "Total: $${"%.2f".format(grandTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    ) { padding ->

        if (selectedItems.isEmpty()) {
            /* -------------------------------------------
               Nothing selected (edge‑case, shouldn’t happen
               if you disabled Checkout button correctly)
            ------------------------------------------- */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No items in cart")
            }
        } else {
            /* -------------------------
               List of selected products
            ------------------------- */
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
                            /*  --- Image placeholder (same style as SalesPage) --- */
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

                            /*  --- Product info + per‑item total & qty  --- */
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

                            /*  --- Qty & line total on the right --- */
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

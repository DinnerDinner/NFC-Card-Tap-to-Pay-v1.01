package com.example.nfccardtaptopayv101.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.EditDeleteProductUiState
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.EditDeleteProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeleteProductScreen(
    sku: String,
    vm: EditDeleteProductViewModel = viewModel(),
    onBack: () -> Unit
) {
    LaunchedEffect(sku) {
        vm.setSku(sku)
    }
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    val scroll = rememberScrollState()

    val title by vm.title.collectAsState()
    val price by vm.price.collectAsState()
    val description by vm.description.collectAsState()
    val keywords by vm.keywords.collectAsState()
    val sku by vm.sku.collectAsState()

    val keywordsRaw = remember { mutableStateOf(keywords.joinToString(", ")) }

    LaunchedEffect(keywords) {
        keywordsRaw.value = keywords.joinToString(", ")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit/Delete Product") },
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
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = { vm.title.value = it },
                label = { Text("Product Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = price,
                onValueChange = { vm.price.value = it },
                label = { Text("Price (CAD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = sku ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("SKU") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { vm.description.value = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Keywords (comma-separated)", fontSize = 14.sp, color = Color.Gray)
                BasicTextField(
                    value = keywordsRaw.value,
                    onValueChange = {
                        keywordsRaw.value = it
                        vm.updateKeywordsFromRaw(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F1F1), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    textStyle = TextStyle(fontSize = 16.sp, color = Color.Black)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                    .clickable { /* TODO: implement image picker */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = "Upload Image")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { vm.submitDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = { vm.submitEdit() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text("Edit")
                }
            }

            when (state) {
                is EditDeleteProductUiState.Error -> {
                    val msg = (state as EditDeleteProductUiState.Error).msg
                    LaunchedEffect(msg) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
                EditDeleteProductUiState.SuccessEdit -> {
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "✅ Product updated", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
                EditDeleteProductUiState.SuccessDelete -> {
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "✅ Product deleted", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                }
                else -> {}
            }
        }
    }
}

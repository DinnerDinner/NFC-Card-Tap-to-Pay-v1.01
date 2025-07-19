package com.example.nfccardtaptopayv101.ui.screens

import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.EditDeleteProductUiState
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.EditDeleteProductViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeleteProductScreen(
    productId: Int,
    vm: EditDeleteProductViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(productId) {
        vm.setProductId(productId)
    }

    val state by vm.uiState.collectAsState()
    val imageUrl by vm.imageUrl.collectAsState()
    val title by vm.title.collectAsState()
    val price by vm.price.collectAsState()
    val description by vm.description.collectAsState()
    val keywords by vm.keywords.collectAsState()
    val sku by vm.sku.collectAsState()
    val uiState by vm.uiState.collectAsState()
    val keywordsRaw = remember { mutableStateOf(keywords.joinToString(", ")) }

    LaunchedEffect(keywords) {
        keywordsRaw.value = keywords.joinToString(", ")
    }

    val scroll = rememberScrollState()

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let {

            vm.uploadImage(it)

        }
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
                    .background(Color.LightGray, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Upload Image",
                        modifier = Modifier.fillMaxSize(0.8f)
                    )
                }
                if (uiState == EditDeleteProductUiState.UploadingImage) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                IconButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.7f), shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Change Image",
                        tint = Color.Black
                    )
                }
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
                    onClick = {
                        if (title.isBlank() || price.isBlank()) {
                            Toast.makeText(context, "Please fill in title and price", Toast.LENGTH_SHORT).show()
                        } else {
                            vm.submitEdit()
                        }
                    },
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
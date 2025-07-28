@file:OptIn(ExperimentalLayoutApi::class)

package com.example.nfccardtaptopayv101.ui.screens.mpos

import androidx.compose.ui.draw.clip
import java.io.File
import androidx.compose.foundation.layout.FlowRow

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    vm: AddProductViewModel = viewModel(),
    managerVM: ProductManagerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var rawKeywords by remember { mutableStateOf("") }
    val uiState by vm.ui.collectAsState()
    val skuScanState by vm.skuScanState.collectAsState()
    val context = LocalContext.current
    var showSkuScanner by remember { mutableStateOf(false) }

    // Camera permission launcher
    var showPermissionDeniedSnackbar by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSkuScanner = true
            vm.startSkuScan()

//        } else {
//            // Permission denied - show error
//            LaunchedEffect(Unit) {
//                snackbarHostState.showSnackbar("Camera permission is required to scan barcodes")
//            }
        }
    }

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(context, it) // helper to get a File from Uri
            if (file != null) {
                vm.uploadImage(file)
            }
        }
    }

    // Flag to trigger snackbar on keyword empty
    var showKeywordError by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                when (uiState) {
                    is AddProductUiState.UploadingImage -> CircularProgressIndicator()
                    is AddProductUiState.Error -> Button(onClick = { launcher.launch("image/*") }) {
                        Text("Retry Upload Image")
                    }
                    is AddProductUiState.Idle, is AddProductUiState.Submitting, is AddProductUiState.Success -> {
                        if (vm.imageUrl != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = rememberAsyncImagePainter(vm.imageUrl),
                                    contentDescription = "Uploaded Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { launcher.launch("image/*") }) {
                                    Text("Change Image")
                                }
                            }
                        } else {
                            Button(onClick = { launcher.launch("image/*") }) {
                                Text("Upload Image")
                            }
                        }
                    }
                }
            }

            val title by vm.title.collectAsState()
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 52) vm.title.value = it
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

            OutlinedTextField(
                value = vm.price.collectAsState().value,
                onValueChange = { vm.price.value = it },
                label = { Text("Price") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // SKU field with scan button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = vm.sku.collectAsState().value,
                    onValueChange = { vm.sku.value = it },
                    label = { Text("SKU (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // Permission already granted
                                showSkuScanner = true
                                vm.startSkuScan()
                            }
                            else -> {
                                // Request permission
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = vm.description.collectAsState().value,
                onValueChange = { vm.description.value = it },
                label = { Text("Description (optional)") },
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
                label = { Text("Keywords (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.keywords.collectAsState().value.forEach {
                    AssistChip(label = { Text(it) }, onClick = {})
                }
            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    if (rawKeywords.trim().isEmpty()) {
                        showKeywordError = true
                    } else {
                        vm.submit()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = uiState !is AddProductUiState.Submitting && uiState !is AddProductUiState.UploadingImage
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

    // SKU Scanner Dialog
    if (showSkuScanner) {
        SkuScannerDialog(
            vm = vm,
            onDismiss = {
                showSkuScanner = false
                vm.stopSkuScan()
            }
        )
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AddProductUiState.Success -> {
                managerVM.refreshProducts()
                delay(300)
                vm.reset()
                onBack()
            }
            is AddProductUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AddProductUiState.Error).msg)
            }
            else -> {}
        }
    }

    LaunchedEffect(skuScanState) {
        when (val currentState = skuScanState) {
            is SkuScanState.Success -> {
                showSkuScanner = false
                vm.stopSkuScan()
                snackbarHostState.showSnackbar("Barcode scanned: ${currentState.barcode}")
            }
            is SkuScanState.Error -> {
                snackbarHostState.showSnackbar(currentState.message)
                vm.resetSkuScanError()
            }
            else -> {}
        }
    }

    LaunchedEffect(showKeywordError) {
        if (showKeywordError) {
            snackbarHostState.showSnackbar("Please enter at least one keyword.")
            showKeywordError = false
        }
    }
}

@Composable
fun SkuScannerDialog(
    vm: AddProductViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder()
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                @androidx.camera.core.ExperimentalGetImage
                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analyzer ->
                                        analyzer.setAnalyzer(cameraExecutor, vm.getSkuImageAnalyzer())
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalyzer
                                )
                            } catch (exc: Exception) {
                                // Log the error or handle it
                                exc.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                // Scanning indicator
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Scanning for barcode...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload", null, context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        null
    }
}





//
// @file:OptIn(ExperimentalLayoutApi::class)
//
//package com.example.nfccardtaptopayv101.ui.screens.mpos
//
//import androidx.compose.ui.draw.clip
//import java.io.File
//import androidx.compose.foundation.layout.FlowRow
//
//import android.net.Uri
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts.GetContent
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//
//import androidx.compose.foundation.verticalScroll
//
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import coil.compose.rememberAsyncImagePainter
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.*
//import kotlinx.coroutines.delay
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.icons.Icons
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.input.KeyboardType
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddProductScreen(
//    vm: AddProductViewModel = viewModel(),
//    managerVM: ProductManagerViewModel = viewModel(),
//    onBack: () -> Unit
//) {
//    val scrollState = rememberScrollState()
//    val snackbarHostState = remember { SnackbarHostState() }
//    var rawKeywords by remember { mutableStateOf("") }
//    val uiState by vm.ui.collectAsState()
//    val context = LocalContext.current
//
//    // Image picker launcher
//    val launcher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
//        uri?.let {
//            val file = uriToFile(context, it) // helper to get a File from Uri
//            if (file != null) {
//                vm.uploadImage(file)
//            }
//        }
//    }
//
//    // Flag to trigger snackbar on keyword empty
//    var showKeywordError by remember { mutableStateOf(false) }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(snackbarHostState) },
//        topBar = {
//            TopAppBar(
//                title = { Text("Add Product") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//
//        Column(
//            Modifier
//                .padding(padding)
//                .padding(16.dp)
//                .verticalScroll(scrollState),
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(180.dp)
//                    .background(Color.LightGray, RoundedCornerShape(12.dp)),
//                contentAlignment = Alignment.Center
//            ) {
//                when (uiState) {
//                    is AddProductUiState.UploadingImage -> CircularProgressIndicator()
//                    is AddProductUiState.Error -> Button(onClick = { launcher.launch("image/*") }) {
//                        Text("Retry Upload Image")
//                    }
//                    is AddProductUiState.Idle, is AddProductUiState.Submitting, is AddProductUiState.Success -> {
//                        if (vm.imageUrl != null) {
//                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                Image(
//                                    painter = rememberAsyncImagePainter(vm.imageUrl),
//                                    contentDescription = "Uploaded Image",
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(180.dp)
//                                        .clip(RoundedCornerShape(12.dp)),
//                                    contentScale = ContentScale.Fit
//                                )
//                                Spacer(Modifier.height(8.dp))
//                                TextButton(onClick = { launcher.launch("image/*") }) {
//                                    Text("Change Image")
//                                }
//                            }
//                        } else {
//                            Button(onClick = { launcher.launch("image/*") }) {
//                                Text("Upload Image")
//                            }
//                        }
//                    }
//                }
//            }
//
//            val title by vm.title.collectAsState()
//            OutlinedTextField(
//                value = title,
//                onValueChange = {
//                    if (it.length <= 52) vm.title.value = it
//                },
//                label = { Text("Product Name") },
//                singleLine = true,
//                modifier = Modifier.fillMaxWidth()
//            )
//            Text(
//                text = "${title.length} / 52",
//                style = MaterialTheme.typography.bodySmall,
//                modifier = Modifier.align(Alignment.End),
//                color = if (title.length >= 52) MaterialTheme.colorScheme.error else Color.Gray
//            )
//
//            OutlinedTextField(
//                value = vm.price.collectAsState().value,
//                onValueChange = { vm.price.value = it },
//                label = { Text("Price") },
//                singleLine = true,
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            OutlinedTextField(
//                value = vm.sku.collectAsState().value,
//                onValueChange = { vm.sku.value = it },
//                label = { Text("SKU (optional)") },
//                singleLine = true,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            OutlinedTextField(
//                value = vm.description.collectAsState().value,
//                onValueChange = { vm.description.value = it },
//                label = { Text("Description (optional)") },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(100.dp)
//            )
//
//            OutlinedTextField(
//                value = rawKeywords,
//                onValueChange = {
//                    rawKeywords = it
//                    vm.updateKeywordsFromRaw(it)
//                },
//                label = { Text("Keywords (comma-separated)") },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                vm.keywords.collectAsState().value.forEach {
//                    AssistChip(label = { Text(it) }, onClick = {})
//                }
//            }
//
//            Spacer(Modifier.height(30.dp))
//
//            Button(
//                onClick = {
//                    if (rawKeywords.trim().isEmpty()) {
//                        showKeywordError = true
//                    } else {
//                        vm.submit()
//                    }
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(55.dp),
//                enabled = uiState !is AddProductUiState.Submitting && uiState !is AddProductUiState.UploadingImage
//            ) {
//                if (uiState is AddProductUiState.Submitting)
//                    CircularProgressIndicator(
//                        modifier = Modifier.size(20.dp),
//                        color = MaterialTheme.colorScheme.onPrimary,
//                        strokeWidth = 2.dp
//                    )
//                else
//                    Text("Add Product")
//            }
//        }
//    }
//
//    LaunchedEffect(uiState) {
//        when (uiState) {
//            is AddProductUiState.Success -> {
//                managerVM.refreshProducts()
//                delay(300)
//                vm.reset()
//                onBack()
//            }
//            is AddProductUiState.Error -> {
//                snackbarHostState.showSnackbar((uiState as AddProductUiState.Error).msg)
//            }
//            else -> {}
//        }
//    }
//
//    LaunchedEffect(showKeywordError) {
//        if (showKeywordError) {
//            snackbarHostState.showSnackbar("Please enter at least one keyword.")
//            showKeywordError = false
//        }
//    }
//}
//
//
//fun uriToFile(context: android.content.Context, uri: Uri): File? {
//    return try {
//        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
//        val tempFile = File.createTempFile("upload", null, context.cacheDir)
//        tempFile.outputStream().use { outputStream ->
//            inputStream.copyTo(outputStream)
//        }
//        tempFile
//    } catch (e: Exception) {
//        null
//    }
//}
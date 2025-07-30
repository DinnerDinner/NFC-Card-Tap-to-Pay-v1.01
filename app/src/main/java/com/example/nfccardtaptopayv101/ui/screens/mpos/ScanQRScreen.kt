@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nfccardtaptopayv101.ui.screens.mpos

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrState
import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrViewModel
import kotlinx.coroutines.launch
@Composable
fun ScanQRScreen(
    vm: ScanQrViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToScannedProduct: () -> Unit
) {

    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // SIMPLIFIED: Only start scan when we can and should
    LaunchedEffect(hasCameraPermission, state) {
        if (hasCameraPermission && vm.canStartScanning()) {
            vm.startScanAfterReset()
        }
    }

    // Handle navigation and state changes
    LaunchedEffect(state) {
        when (state) {
            is ScanQrState.NavigateToScannedProduct -> {
                onNavigateToScannedProduct()
            }
            is ScanQrState.ScanSuccess -> {
                Toast.makeText(
                    context,
                    "✅ Product found!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> { /* no op */ }
        }
    }

    if (!hasCameraPermission) {
        // Show permission request UI
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Camera permission is required to scan barcodes.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("Cancel")
                }
            }
        }
        return
    }

    ScanQRContent(vm = vm, onBack = onBack)
}

@Composable
private fun ScanQRContent(
    vm: ScanQrViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // SIMPLIFIED: Create PreviewView once and reuse
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // SIMPLIFIED: Use a simple boolean to track camera binding
    var isCameraReady by remember { mutableStateOf(false) }

    // SIMPLIFIED: Only bind camera when in detecting state
    LaunchedEffect(state) {
        if (state is ScanQrState.Detecting && !isCameraReady) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                @androidx.camera.core.ExperimentalGetImage
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(ContextCompat.getMainExecutor(context),
                            vm.getImageAnalyzer { barcode ->
                                vm.onBarcodeDetected(barcode)
                            }
                        )
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

                isCameraReady = true
                Log.d("ScanQRScreen", "Camera bound successfully")
            } catch (e: Exception) {
                Log.e("ScanQRScreen", "Camera binding failed: ${e.localizedMessage}")
                isCameraReady = false
            }
        }

        // Unbind camera when not in detecting state
        if (state !is ScanQrState.Detecting && isCameraReady) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
                isCameraReady = false
                Log.d("ScanQRScreen", "Camera unbound")
            } catch (e: Exception) {
                Log.e("ScanQRScreen", "Camera unbinding failed: ${e.localizedMessage}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                val label = when (val s = state) {
                    is ScanQrState.Idle -> "Ready to scan..."
                    is ScanQrState.Detecting -> "Detecting barcode..."
                    is ScanQrState.Detected -> "Code detected: ${s.barcode.take(15)}${if (s.barcode.length > 15) "..." else ""}"
                    is ScanQrState.BackendVerifying -> "Verifying product..."
                    is ScanQrState.ScanSuccess -> "✅ Product found!"
                    is ScanQrState.NavigateToScannedProduct -> "Redirecting..."
                    is ScanQrState.Error -> "❌ ${s.message}"
                }

                Text(
                    text = label,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when (state) {
                        is ScanQrState.Error -> MaterialTheme.colorScheme.error
                        is ScanQrState.ScanSuccess, is ScanQrState.NavigateToScannedProduct -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (!isCameraReady) Color.Black else Color.Transparent)
                )

                // Show loading only when camera is not ready but should be
                if (state is ScanQrState.Detecting && !isCameraReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Starting camera...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Scan box overlay (only show when camera is ready)
                if (isCameraReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1.2f)
                            .border(
                                width = 3.dp,
                                color = when (state) {
                                    is ScanQrState.Detected -> Color.Yellow
                                    is ScanQrState.BackendVerifying -> Color.Blue
                                    is ScanQrState.ScanSuccess, is ScanQrState.NavigateToScannedProduct -> Color.Green
                                    is ScanQrState.Error -> Color.Red
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                shape = MaterialTheme.shapes.medium
                            )
                    )

                    // Dark edges gradient overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }

                // Loading indicator for backend verification
                if (state is ScanQrState.BackendVerifying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Checking product...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            // Instructions text
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Point your camera at a barcode or QR code to scan",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Error retry button
            AnimatedVisibility(
                visible = state is ScanQrState.Error,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut()
            ) {
                Button(
                    onClick = {
                        isCameraReady = false // Reset camera state
                        vm.resetForNavigation()
                        vm.startScanAfterReset()
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text("Try Again")
                }
            }
        }
    }
}
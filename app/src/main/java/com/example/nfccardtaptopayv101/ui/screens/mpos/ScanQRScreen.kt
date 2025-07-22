@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nfccardtaptopayv101.ui.screens.mpos
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
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
    onSuccessScan: (String) -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state) {
        when (val currentState = state) {
            is ScanQrState.ScanSuccess -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                onSuccessScan(currentState.message)
            }
            is ScanQrState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
            }
            else -> { /* Handle other states */ }
        }
    }
    if (!hasCameraPermission) {
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
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("Cancel")
                }
            }
        }
        return
    }

    // Permission granted, show scanning UI
    ScanQRContent(vm, onBack, onSuccessScan)
}

@Composable
fun ScanQRContent(
    vm: ScanQrViewModel,
    onBack: () -> Unit,
    onSuccessScan: (String) -> Unit
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val previewView = remember { PreviewView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        scaleType = PreviewView.ScaleType.FILL_CENTER
    }}

    var cameraBound by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.startScan()

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
                            coroutineScope.launch {
                                onSuccessScan(barcode)
                            }
                        })
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            cameraBound = true
        } catch (e: Exception) {
            Log.e("ScanQrScreen", "Camera binding failed: ${e.localizedMessage}")
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
                    is ScanQrState.Detecting -> "Detecting..."
                    is ScanQrState.Detected -> "Detected: ${s.barcode}"
                    is ScanQrState.BackendVerifying -> "Verifying barcode..."
                    is ScanQrState.RedirectReady -> "Redirecting..."
                    is ScanQrState.Error -> "Error: ${s.message}"
                    else -> ""
                }
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
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
                        .background(Color.Black)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1.2f)
                        .border(3.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium)
                )

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

            AnimatedVisibility(
                visible = state is ScanQrState.Error,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut()
            ) {
                Text(
                    text = (state as? ScanQrState.Error)?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}














//package com.example.nfccardtaptopayv101.ui.screens.mpos
//import android.Manifest
//import android.annotation.SuppressLint
//import android.util.Log
//import android.view.ViewGroup
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.compose.animation.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.*
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrState
//import com.example.nfccardtaptopayv101.ui.viewmodel.mpos.ScanQrViewModel
//import kotlinx.coroutines.launch
//import androidx.compose.foundation.border
//import androidx.compose.foundation.BorderStroke
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ScanQRScreen(
//    vm: ScanQrViewModel = viewModel(),
//    onBack: () -> Unit,
//    onSuccessScan: (String) -> Unit
//) {
//    val state by vm.state.collectAsState()
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val coroutineScope = rememberCoroutineScope()
//
//    // PreviewView reference to hold camera surface
//    val previewView = remember { PreviewView(context).apply {
//        layoutParams = ViewGroup.LayoutParams(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        )
//        scaleType = PreviewView.ScaleType.FILL_CENTER
//    }}
//
//    var cameraBound by remember { mutableStateOf(false) }
//
//    LaunchedEffect(Unit) {
//        vm.startScan()
//
//        try {
//            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//            @androidx.camera.core.ExperimentalGetImage
//            val imageAnalysis = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//                .apply {
//                    setAnalyzer(ContextCompat.getMainExecutor(context),
//                        vm.getImageAnalyzer { barcode ->
//                            coroutineScope.launch {
//                                onSuccessScan(barcode)
//                            }
//                        })
//                }
//
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                lifecycleOwner,
//                CameraSelector.DEFAULT_BACK_CAMERA,
//                preview,
//                imageAnalysis
//            )
//
//            cameraBound = true
//        } catch (e: Exception) {
//            Log.e("ScanQrScreen", "Camera binding failed: ${e.localizedMessage}")
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Scan Barcode") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar {
//                val label = when (val s = state) {
//                    is ScanQrState.Detecting -> "Detecting..."
//                    is ScanQrState.Detected -> "Detected: ${s.barcode}"
//                    is ScanQrState.BackendVerifying -> "Verifying barcode..."
//                    is ScanQrState.RedirectReady -> "Redirecting..."
//                    is ScanQrState.Error -> "Error: ${s.message}"
//                    else -> ""
//                }
//                Text(
//                    text = label,
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .fillMaxWidth(),
//                    style = MaterialTheme.typography.bodyLarge
//                )
//            }
//        }
//    ) { padding ->
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .background(MaterialTheme.colorScheme.surface)
//        ) {
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f)
//                    .padding(16.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                AndroidView(
//                    factory = { previewView },
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(Color.Black)
//                )
//
//                // Fancy gradient scan box overlay
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth(0.85f)
//                        .aspectRatio(1.2f)
//                        .border(3.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium)
//                )
//
//                // Darken edges
//                Box(
//                    modifier = Modifier
//                        .matchParentSize()
//                        .background(
//                            Brush.verticalGradient(
//                                colors = listOf(
//                                    Color.Black.copy(alpha = 0.5f),
//                                    Color.Transparent,
//                                    Color.Black.copy(alpha = 0.6f)
//                                )
//                            )
//                        )
//                )
//            }
//
//            AnimatedVisibility(
//                visible = state is ScanQrState.Error,
//                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
//                exit = fadeOut()
//            ) {
//                Text(
//                    text = (state as? ScanQrState.Error)?.message ?: "",
//                    color = MaterialTheme.colorScheme.error,
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .align(Alignment.CenterHorizontally)
//                )
//            }
//        }
//    }
//}

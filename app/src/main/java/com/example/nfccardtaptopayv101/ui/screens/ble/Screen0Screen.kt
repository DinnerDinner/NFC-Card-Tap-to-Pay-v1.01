package com.example.nfccardtaptopayv101.ui.screens.ble

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.ProfileGatekeeperUiState
import com.example.nfccardtaptopayv101.ui.viewmodel.ble.Screen0ViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen0Screen(
    onNavigateToModeSelection: () -> Unit,
    viewModel: Screen0ViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // State for handling camera
    var photoFile by remember { mutableStateOf<File?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Check camera permission on composition
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Handle navigation
    LaunchedEffect(uiState) {
        if (uiState is ProfileGatekeeperUiState.NavigateToModeSelection) {
            viewModel.clearNavigationFlag()
            onNavigateToModeSelection()
        }
    }

    // Camera launcher using TakePicture contract
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null && photoFile!!.exists()) {
            viewModel.uploadProfilePicture(photoFile!!)
        }
    }

    // Define launchCamera function after cameraLauncher is declared
    fun launchCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "profile_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

            // Ensure directory exists
            if (storageDir != null && !storageDir.exists()) {
                storageDir.mkdirs()
            }

            val file = File.createTempFile(imageFileName, ".jpg", storageDir)
            photoFile = file

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            photoUri = uri

            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error gracefully - you might want to show a snackbar or error message
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            launchCamera()
        }
    }

    // Define handleCameraClick function after permissionLauncher is declared
    fun handleCameraClick() {
        if (hasCameraPermission) {
            launchCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    when (uiState) {
        is ProfileGatekeeperUiState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is ProfileGatekeeperUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Checking profile status...")
                }
            }
        }

        is ProfileGatekeeperUiState.ShowCaptureUI -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Profile Picture Placeholder - Made bigger
                Card(
                    modifier = Modifier
                        .size(320.dp)
                        .clip(CircleShape),
                    onClick = { handleCameraClick() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            // Show captured image if available, otherwise show camera icon
                            if (photoUri != null && photoFile?.exists() == true) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Captured Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(100.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Tap to Take Your Profile Picture",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "This helps us verify your presence today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // Red Capture Button
                ExtendedFloatingActionButton(
                    onClick = { handleCameraClick() },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoFile?.exists() == true) "Retake Photo" else "Take Photo")
                }
            }
        }

        is ProfileGatekeeperUiState.Uploading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Uploading profile picture...")

                    // Show the captured image while uploading
                    if (photoUri != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                        ) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Uploading Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        is ProfileGatekeeperUiState.NavigateToModeSelection -> {
            // This state triggers navigation via LaunchedEffect
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Navigating...")
            }
        }

        is ProfileGatekeeperUiState.Error -> {
            val errorState = uiState as ProfileGatekeeperUiState.Error
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${errorState.msg}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.checkProfilePictureStatus() }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
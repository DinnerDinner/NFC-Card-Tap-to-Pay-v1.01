package com.example.nfccardtaptopayv101.ui.screens.ble

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
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

    // Check profile picture status on launch
    LaunchedEffect(Unit) {
        viewModel.checkProfilePictureStatus()
    }

    // Handle navigation
    LaunchedEffect(uiState) {
        if (uiState is ProfileGatekeeperUiState.NavigateToModeSelection) {
            viewModel.clearNavigationFlag()
            onNavigateToModeSelection()
        }
    }

    // Camera launcher
    var photoFile by remember { mutableStateOf<File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoFile?.let { file ->
                if (file.exists()) {
                    viewModel.uploadProfilePicture(file)
                }
            }
        }
    }

    fun launchCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

        try {
            val file = File.createTempFile(imageFileName, ".jpg", storageDir)
            photoFile = file

            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            // Handle error silently for now
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
                // Profile Picture Placeholder
                Card(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape),
                    onClick = { launchCamera() }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    onClick = { launchCamera() },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
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
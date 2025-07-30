package com.example.nfccardtaptopayv101.ui.viewmodel.ble

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"

sealed class ProfileGatekeeperUiState {
    object Idle : ProfileGatekeeperUiState()
    object Loading : ProfileGatekeeperUiState()
    object ShowCaptureUI : ProfileGatekeeperUiState()
    object Uploading : ProfileGatekeeperUiState()
    object NavigateToModeSelection : ProfileGatekeeperUiState()
    data class Error(val msg: String) : ProfileGatekeeperUiState()
}

class Screen0ViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<ProfileGatekeeperUiState>(ProfileGatekeeperUiState.Idle)
    val uiState: StateFlow<ProfileGatekeeperUiState> = _uiState

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Updated client configuration to match your AddProductViewModel
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var currentUserId: String = ""

    // Method to set userId externally
    fun setUserId(userId: String) {
        Log.d("Screen0ViewModel", "Setting userId: $userId")
        currentUserId = userId
        // Start checking status immediately when userId is set
        if (userId.isNotEmpty()) {
            checkProfilePictureStatus()
        } else {
            Log.e("Screen0ViewModel", "Empty userId provided")
            _uiState.value = ProfileGatekeeperUiState.Error("Invalid user ID")
        }
    }

    private fun getUserId(): String {
        return if (currentUserId.isNotEmpty()) {
            currentUserId
        } else {
            prefs.getString("user_id", "") ?: ""
        }
    }

    fun checkProfilePictureStatus() {
        val userId = getUserId()
        Log.d("Screen0ViewModel", "Checking profile status for userId: $userId")

        if (userId.isEmpty()) {
            Log.e("Screen0ViewModel", "User ID is empty")
            _uiState.value = ProfileGatekeeperUiState.Error("User ID not found")
            return
        }

        _uiState.value = ProfileGatekeeperUiState.Loading

        viewModelScope.launch {
            try {
                val req = Request.Builder()
                    .url("$BASE_URL/user/profile_picture_status?user_id=$userId")
                    .get()
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(req).execute()
                }.use { response ->
                    Log.d("Screen0ViewModel", "Response code: ${response.code}")

                    if (response.isSuccessful) {
                        val raw = response.body?.string() ?: "{}"
                        Log.d("Screen0ViewModel", "Response body: $raw")

                        val json = JSONObject(raw)
                        val hasProfilePicture = json.optBoolean("has_profile_picture", false)
                        val imageUrl = json.optString("image_url", "")

                        _uiState.value = if (hasProfilePicture && imageUrl.isNotEmpty()) {
                            Log.d("Screen0ViewModel", "User has profile picture, navigating to mode selection")
                            ProfileGatekeeperUiState.NavigateToModeSelection
                        } else {
                            Log.d("Screen0ViewModel", "User needs to capture profile picture")
                            ProfileGatekeeperUiState.ShowCaptureUI
                        }
                    } else {
                        Log.e("Screen0ViewModel", "Server error: ${response.code}")
                        _uiState.value = ProfileGatekeeperUiState.Error("Server error: ${response.code}")
                    }
                }
            } catch (e: IOException) {
                Log.e("Screen0ViewModel", "Network error checking profile", e)
                _uiState.value = ProfileGatekeeperUiState.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e("Screen0ViewModel", "Unexpected error checking profile", e)
                _uiState.value = ProfileGatekeeperUiState.Error("Unexpected error: ${e.message}")
            }
        }
    }

    // Updated upload method following AddProductViewModel pattern
    fun uploadProfilePicture(imageFile: File) {
        val userId = getUserId()
        Log.d("Screen0ViewModel", "Uploading profile picture for userId: $userId")

        if (userId.isEmpty()) {
            Log.e("Screen0ViewModel", "User ID is empty during upload")
            _uiState.value = ProfileGatekeeperUiState.Error("User ID not found")
            return
        }

        if (!imageFile.exists()) {
            Log.e("Screen0ViewModel", "Image file does not exist: ${imageFile.absolutePath}")
            _uiState.value = ProfileGatekeeperUiState.Error("Image file not found")
            return
        }

        _uiState.value = ProfileGatekeeperUiState.Uploading

        viewModelScope.launch {
            try {
                // Compress image like in AddProductViewModel
                val compressedFile = withContext(Dispatchers.IO) {
                    compressImage(getApplication(), imageFile)
                }

                Log.d("Screen0ViewModel", "Compressed file size: ${compressedFile.length()} bytes")

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("user_id", userId)
                    .addFormDataPart(
                        "image", // Changed from "profile_picture" to "image" to match your backend
                        compressedFile.name,
                        compressedFile.asRequestBody("image/*".toMediaType())
                    )
                    .build()

                val req = Request.Builder()
                    .url("$BASE_URL/user/upload_profile_picture")
                    .post(requestBody)
                    .build()

                withContext(Dispatchers.IO) {
                    client.newCall(req).execute()
                }.use { response ->
                    Log.d("Screen0ViewModel", "Upload response code: ${response.code}")

                    if (response.isSuccessful) {
                        val raw = response.body?.string()
                        Log.d("Screen0ViewModel", "Upload response body: $raw")

                        if (raw != null) {
                            val json = JSONObject(raw)
                            val imageUrl = json.optString("image_url", "")
                            val returnedUserId = json.optString("user_id", "")

                            if (imageUrl.isNotEmpty() && returnedUserId == userId) {
                                Log.d("Screen0ViewModel", "Profile picture uploaded successfully")
                                _uiState.value = ProfileGatekeeperUiState.NavigateToModeSelection
                            } else {
                                Log.e("Screen0ViewModel", "Upload response invalid - imageUrl: $imageUrl, returnedUserId: $returnedUserId")
                                _uiState.value = ProfileGatekeeperUiState.Error("Upload response invalid")
                            }
                        } else {
                            Log.e("Screen0ViewModel", "Empty response from server")
                            _uiState.value = ProfileGatekeeperUiState.Error("Empty response from server")
                        }
                    } else {
                        Log.e("Screen0ViewModel", "Upload failed with code: ${response.code}")
                        _uiState.value = ProfileGatekeeperUiState.Error("Upload failed: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e("Screen0ViewModel", "Upload failed", e)
                _uiState.value = ProfileGatekeeperUiState.Error("Upload error: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        _uiState.value = ProfileGatekeeperUiState.Idle
    }

    fun clearNavigationFlag() {
        if (_uiState.value is ProfileGatekeeperUiState.NavigateToModeSelection) {
            _uiState.value = ProfileGatekeeperUiState.Idle
        }
    }
}

// Image compression function (copied from AddProductViewModel) with better error handling
private fun compressImage(context: Context, file: File): File {
    try {
        val bitmap = BitmapFactory.decodeFile(file.path)
            ?: throw IllegalArgumentException("Could not decode image file")

        val compressedFile = File.createTempFile("compressed", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(compressedFile)

        // Compress with quality 75 (adjust as needed)
        val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        outputStream.close()

        if (!compressed) {
            throw IOException("Failed to compress image")
        }

        Log.d("Screen0ViewModel", "Image compressed: ${file.length()} -> ${compressedFile.length()} bytes")
        return compressedFile
    } catch (e: Exception) {
        Log.e("Screen0ViewModel", "Error compressing image", e)
        throw e
    }
}
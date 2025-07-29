package com.example.nfccardtaptopayv101.ui.viewmodel.ble

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
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
    private val client = OkHttpClient()

    private var currentUserId: String = ""

    // Method to set userId externally
    fun setUserId(userId: String) {
        currentUserId = userId
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
        if (userId.isEmpty()) {
            _uiState.value = ProfileGatekeeperUiState.Error("User ID not found")
            return
        }

        _uiState.value = ProfileGatekeeperUiState.Loading

        viewModelScope.launch {
            val req = Request.Builder()
                .url("$BASE_URL/user/profile_picture_status?user_id=$userId")
                .get()
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = ProfileGatekeeperUiState.Error("Network error: ${e.message}")
                }

                override fun onResponse(call: Call, res: Response) {
                    if (res.isSuccessful) {
                        val raw = res.body?.string() ?: "{}"
                        val json = JSONObject(raw)
                        val hasProfilePicture = json.optBoolean("has_profile_picture", false)
                        val imageUrl = json.optString("image_url", "")

                        _uiState.value = if (hasProfilePicture && imageUrl.isNotEmpty()) {
                            ProfileGatekeeperUiState.NavigateToModeSelection
                        } else {
                            ProfileGatekeeperUiState.ShowCaptureUI
                        }
                    } else {
                        _uiState.value = ProfileGatekeeperUiState.Error("Server error: ${res.code}")
                    }
                }
            })
        }
    }

    fun uploadProfilePicture(imageFile: File) {
        val userId = getUserId()
        if (userId.isEmpty()) {
            _uiState.value = ProfileGatekeeperUiState.Error("User ID not found")
            return
        }

        _uiState.value = ProfileGatekeeperUiState.Uploading

        viewModelScope.launch {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", userId)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    RequestBody.create("image/*".toMediaType(), imageFile)
                )
                .build()

            val req = Request.Builder()
                .url("$BASE_URL/user/upload_profile_picture")
                .post(requestBody)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = ProfileGatekeeperUiState.Error("Upload failed: ${e.message}")
                }

                override fun onResponse(call: Call, res: Response) {
                    if (res.isSuccessful) {
                        val raw = res.body?.string() ?: "{}"
                        val json = JSONObject(raw)
                        val imageUrl = json.optString("image_url", "")
                        val returnedUserId = json.optString("user_id", "")

                        if (imageUrl.isNotEmpty() && returnedUserId == userId) {
                            _uiState.value = ProfileGatekeeperUiState.NavigateToModeSelection
                        } else {
                            _uiState.value = ProfileGatekeeperUiState.Error("Upload response invalid")
                        }
                    } else {
                        _uiState.value = ProfileGatekeeperUiState.Error("Server error: ${res.code}")
                    }
                }
            })
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
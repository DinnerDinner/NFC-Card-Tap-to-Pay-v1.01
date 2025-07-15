package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.app.Application
import android.content.Context
import android.nfc.Tag
import android.nfc.tech.NfcA
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

sealed class NfcTapUiState {
    object Waiting : NfcTapUiState()
    object Tapped : NfcTapUiState()
    data class Success(val message: String) : NfcTapUiState()
    data class Error(val message: String) : NfcTapUiState()
}

class NfcTapViewModel(app: Application) : AndroidViewModel(app) {

    private val client = OkHttpClient()
    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<NfcTapUiState>(NfcTapUiState.Waiting)
    val uiState: StateFlow<NfcTapUiState> = _uiState.asStateFlow()

    var totalAmount: Double = 0.0

    fun onCardTapped(tag: Tag) {
        val uid = tag.id?.joinToString("") { "%02X".format(it) } ?: return
        val merchantId = prefs.getInt("user_id", -1)

        if (merchantId == -1) {
            _uiState.value = NfcTapUiState.Error("Merchant ID missing")
            return
        }

        val json = JSONObject().apply {
            put("uid", uid)
            put("merchant_id", merchantId)
            put("amount", totalAmount)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://promoted-quetzal-visually.ngrok-free.app/transfer")
            .post(body)
            .build()

        viewModelScope.launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "{}")

                if (response.isSuccessful) {
                    _uiState.value = NfcTapUiState.Success(
                        jsonResponse.optString("message", "✅ Payment successful")
                    )
                } else {
                    _uiState.value = NfcTapUiState.Error(
                        jsonResponse.optString("detail", "❌ Payment failed")
                    )
                }
            } catch (e: IOException) {
                _uiState.value = NfcTapUiState.Error("❌ Network error: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        _uiState.value = NfcTapUiState.Waiting
        totalAmount = 0.0
    }
}

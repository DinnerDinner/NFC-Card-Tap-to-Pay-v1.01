package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val BASE_URL = "https://promoted-quetzal-visually.ngrok-free.app"

sealed class AddProductUiState {
    object Idle : AddProductUiState()
    object Submitting : AddProductUiState()
    object Success : AddProductUiState()
    data class Error(val msg: String) : AddProductUiState()
}

class AddProductViewModel(app: Application) : AndroidViewModel(app) {

    val title       = MutableStateFlow("")
    val price       = MutableStateFlow("")
    val sku         = MutableStateFlow("")
    val description = MutableStateFlow("")
    val keywords    = MutableStateFlow<List<String>>(emptyList())
    var imageUrl: String? = null

    private val _ui = MutableStateFlow<AddProductUiState>(AddProductUiState.Idle)
    val ui: StateFlow<AddProductUiState> = _ui

    private val prefs  = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun updateKeywordsFromRaw(raw: String) {
        keywords.value = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun submit() {
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            _ui.value = AddProductUiState.Error("User ID missing")
            return
        }

        val priceNumber = price.value.toDoubleOrNull()
        if (title.value.isBlank() || priceNumber == null || priceNumber <= 0.0) {
            _ui.value = AddProductUiState.Error("Valid title & price required")
            return
        }

        _ui.value = AddProductUiState.Submitting

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("title", title.value.trim())
                put("price", "%.2f".format(priceNumber))
                put("sku", sku.value.trim().ifBlank { JSONObject.NULL })
                put("description", description.value.trim().ifBlank { JSONObject.NULL })
                put("keywords", JSONArray(keywords.value))
                put("image_url", imageUrl ?: JSONObject.NULL)
            }

            val req = Request.Builder()
                .url("$BASE_URL/products/create")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _ui.value = AddProductUiState.Error("Network error")
                }

                override fun onResponse(call: Call, res: Response) {
                    _ui.value = if (res.isSuccessful)
                        AddProductUiState.Success
                    else
                        AddProductUiState.Error("Server error: ${res.message}")
                }
            })
        }
    }

    fun reset() {
        title.value = ""
        price.value = ""
        sku.value = ""
        description.value = ""
        keywords.value = emptyList()
        imageUrl = null
        _ui.value = AddProductUiState.Idle
    }
}

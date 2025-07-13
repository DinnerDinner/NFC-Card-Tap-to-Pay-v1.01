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

sealed class EditDeleteProductUiState {
    object Idle : EditDeleteProductUiState()
    object Loading : EditDeleteProductUiState()
    object SubmittingEdit : EditDeleteProductUiState()
    object SubmittingDelete : EditDeleteProductUiState()
    object SuccessEdit : EditDeleteProductUiState()
    object SuccessDelete : EditDeleteProductUiState()
    data class Error(val msg: String) : EditDeleteProductUiState()
}

class EditDeleteProductViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    // SKU is passed/set externally (e.g., from ProductManager screen or saved state)
    private val _sku = MutableStateFlow<String?>(null)
    val sku: StateFlow<String?> = _sku.asStateFlow()

    // Fields to populate the UI
    val title = MutableStateFlow("")
    val price = MutableStateFlow("")
    val description = MutableStateFlow("")
    val keywords = MutableStateFlow<List<String>>(emptyList())
    val skuField = MutableStateFlow("")  // SKU field to show (usually uneditable)
    var imageUrl: String? = null

    private val _uiState = MutableStateFlow<EditDeleteProductUiState>(EditDeleteProductUiState.Idle)
    val uiState: StateFlow<EditDeleteProductUiState> = _uiState.asStateFlow()

    fun setSku(sku: String) {
        _sku.value = sku
        skuField.value = sku
        fetchProductDetails(sku)
    }

    fun updateKeywordsFromRaw(raw: String) {
        keywords.value = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun fetchProductDetails(sku: String) {
        _uiState.value = EditDeleteProductUiState.Loading
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            _uiState.value = EditDeleteProductUiState.Error("User ID missing")
            return
        }

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("sku", sku)
            }
            val req = Request.Builder()
                .url("$BASE_URL/products/get_by_sku")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = EditDeleteProductUiState.Error("Network error: ${e.message}")
                }

                override fun onResponse(call: Call, res: Response) {
                    if (res.isSuccessful) {
                        val bodyStr = res.body?.string() ?: ""
                        try {
                            val json = JSONObject(bodyStr)
                            // Example keys: title, price, description, keywords (JSONArray), image_url
                            title.value = json.optString("title", "")
                            price.value = json.optDouble("price", 0.0).toString()
                            description.value = json.optString("description", "")
                            imageUrl = json.optString("image_url", null)
                            val keywordsJson = json.optJSONArray("keywords")
                            val keywordsList = mutableListOf<String>()
                            if (keywordsJson != null) {
                                for (i in 0 until keywordsJson.length()) {
                                    keywordsList.add(keywordsJson.getString(i))
                                }
                            }
                            keywords.value = keywordsList

                            _uiState.value = EditDeleteProductUiState.Idle
                        } catch (e: Exception) {
                            _uiState.value = EditDeleteProductUiState.Error("Malformed response")
                        }
                    } else {
                        _uiState.value = EditDeleteProductUiState.Error("Server error: ${res.message}")
                    }
                }
            })
        }
    }

    fun submitEdit() {
        val userId = prefs.getInt("user_id", -1)
        val skuVal = sku.value ?: ""
        val priceNumber = price.value.toDoubleOrNull()
        if (userId == -1) {
            _uiState.value = EditDeleteProductUiState.Error("User ID missing")
            return
        }
        if (skuVal.isBlank() || title.value.isBlank() || priceNumber == null || priceNumber <= 0.0) {
            _uiState.value = EditDeleteProductUiState.Error("Valid SKU, title & price required")
            return
        }

        _uiState.value = EditDeleteProductUiState.SubmittingEdit

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("sku", skuVal)
                put("title", title.value.trim())
                put("price", "%.2f".format(priceNumber))
                put("description", description.value.trim().ifBlank { JSONObject.NULL })
                put("keywords", JSONArray(keywords.value))
                put("image_url", imageUrl ?: JSONObject.NULL)
            }
            val req = Request.Builder()
                .url("$BASE_URL/products/edit")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = EditDeleteProductUiState.Error("Network error: ${e.message}")
                }

                override fun onResponse(call: Call, res: Response) {
                    _uiState.value = if (res.isSuccessful) {
                        EditDeleteProductUiState.SuccessEdit
                    } else {
                        EditDeleteProductUiState.Error("Server error: ${res.message}")
                    }
                }
            })
        }
    }

    fun submitDelete() {
        val userId = prefs.getInt("user_id", -1)
        val skuVal = sku.value ?: ""

        if (userId == -1) {
            _uiState.value = EditDeleteProductUiState.Error("User ID missing")
            return
        }
        if (skuVal.isBlank()) {
            _uiState.value = EditDeleteProductUiState.Error("SKU is missing")
            return
        }

        _uiState.value = EditDeleteProductUiState.SubmittingDelete

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("sku", skuVal)
            }
            val req = Request.Builder()
                .url("$BASE_URL/products/delete")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = EditDeleteProductUiState.Error("Network error: ${e.message}")
                }

                override fun onResponse(call: Call, res: Response) {
                    _uiState.value = if (res.isSuccessful) {
                        EditDeleteProductUiState.SuccessDelete
                    } else {
                        EditDeleteProductUiState.Error("Server error: ${res.message}")
                    }
                }
            })
        }
    }
}

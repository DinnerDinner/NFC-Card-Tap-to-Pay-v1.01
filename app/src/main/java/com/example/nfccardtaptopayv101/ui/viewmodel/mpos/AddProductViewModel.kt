package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"

sealed class AddProductUiState {
    object Idle : AddProductUiState()
    object UploadingImage : AddProductUiState()
    object Submitting : AddProductUiState()
    data class Success(val productId: Int) : AddProductUiState()
    data class Error(val msg: String) : AddProductUiState()
}

class AddProductViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    val title       = MutableStateFlow("")
    val price       = MutableStateFlow("")
    val sku         = MutableStateFlow("")
    val description = MutableStateFlow("")
    val keywords    = MutableStateFlow<List<String>>(emptyList())

    private var _imageUrl: String? = null
    val imageUrl get() = _imageUrl
    val productId = MutableStateFlow<Int?>(null)

    private val _ui = MutableStateFlow<AddProductUiState>(AddProductUiState.Idle)
    val ui: StateFlow<AddProductUiState> = _ui

    private val client = OkHttpClient.Builder()
        .connectTimeout(7, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun updateKeywordsFromRaw(raw: String) {
        keywords.value = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    fun uploadImage(file: File, retryCount: Int = 1) {
        _ui.value = AddProductUiState.UploadingImage

        val compressedFile = compressImage(getApplication(), file)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                compressedFile.name,
                compressedFile.asRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/upload_image")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (retryCount > 0) {
                    uploadImage(file, retryCount - 1)  // Retry once
                } else {
                    _ui.value = AddProductUiState.Error("Image upload failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (!response.isSuccessful || bodyStr == null) {
                    _ui.value = AddProductUiState.Error("Image upload failed: ${response.message}")
                    return
                }
                try {
                    val json = JSONObject(bodyStr)
                    val url = json.optString("image_url", null)
                    if (url == null) {
                        _ui.value = AddProductUiState.Error("Image upload failed: No URL returned")
                        return
                    }
                    _imageUrl = url
                    _ui.value = AddProductUiState.Idle
                } catch (ex: Exception) {
                    _ui.value = AddProductUiState.Error("Image upload failed: ${ex.message}")
                }
            }
        })
    }

    private fun tryUpload(request: Request, retriesLeft: Int) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (retriesLeft > 0) {
                    tryUpload(request, retriesLeft - 1)
                } else {
                    _ui.value = AddProductUiState.Error("Image upload failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    if (retriesLeft > 0) {
                        tryUpload(request, retriesLeft - 1)
                    } else {
                        _ui.value = AddProductUiState.Error("Upload failed: ${response.message}")
                    }
                    return
                }

                val bodyStr = response.body?.string()
                val imageUrl = runCatching { JSONObject(bodyStr).optString("image_url", null) }.getOrNull()

                if (imageUrl != null) {
                    _imageUrl = imageUrl
                    _ui.value = AddProductUiState.Idle
                } else {
                    _ui.value = AddProductUiState.Error("Upload failed: No image URL returned")
                }
            }
        })
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
                put("image_url", _imageUrl ?: JSONObject.NULL)
            }

            val request = Request.Builder()
                .url("$BASE_URL/products/create")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _ui.value = AddProductUiState.Error("Submission failed: ${e.message}")
                }

                override fun onResponse(call: Call, res: Response) {
                    val bodyStr = res.body?.string()
                    if (!res.isSuccessful || bodyStr == null) {
                        _ui.value = AddProductUiState.Error("Server error: ${res.message}")
                        return
                    }

                    try {
                        val json = JSONObject(bodyStr)
                        val id = json.optInt("id", -1) // productId returned from backend
                        if (id == -1) {
                            _ui.value = AddProductUiState.Error("Missing product id in response")
                        } else {
                            productId.value = id  // Store it for other viewmodels or usages
                            _ui.value = AddProductUiState.Success(id)  // Pass productId here to Success
                        }
                    } catch (e: Exception) {
                        _ui.value = AddProductUiState.Error("Invalid server response: ${e.message}")
                    }
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
        _imageUrl = null
        _ui.value = AddProductUiState.Idle
    }
}

private fun compressImage(context: Context, file: File): File {
    val bitmap = android.graphics.BitmapFactory.decodeFile(file.path)
    val compressedFile = File.createTempFile("compressed", ".jpg", context.cacheDir)
    val outputStream = compressedFile.outputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
    outputStream.close()
    return compressedFile
}

package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
import okhttp3.RequestBody.Companion.asRequestBody

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

private const val BASE_URL = "https://promoted-quetzal-visually.ngrok-free.app"

sealed class EditDeleteProductUiState {
    object Idle : EditDeleteProductUiState()
    object Loading : EditDeleteProductUiState()
    object SubmittingEdit : EditDeleteProductUiState()
    object SubmittingDelete : EditDeleteProductUiState()
    object SuccessEdit : EditDeleteProductUiState()
    object SuccessDelete : EditDeleteProductUiState()
    object UploadingImage : EditDeleteProductUiState() // ← You need this
    data class Error(val msg: String) : EditDeleteProductUiState()
}
 class EditDeleteProductViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    // Product ID (new main identifier), nullable at first
    private val _productId = MutableStateFlow<Int?>(null)
    val productId: StateFlow<Int?> = _productId.asStateFlow()

    // SKU and other product details, just fields now
    val sku = MutableStateFlow("")
    val title = MutableStateFlow("")
    val price = MutableStateFlow("")
    val description = MutableStateFlow("")
    val keywords = MutableStateFlow<List<String>>(emptyList())
    private val _imageUrl = MutableStateFlow<String?>(null)
    val imageUrl: StateFlow<String?> = _imageUrl
    private val _uiState = MutableStateFlow<EditDeleteProductUiState>(EditDeleteProductUiState.Idle)
    val uiState: StateFlow<EditDeleteProductUiState> = _uiState.asStateFlow()

    // Set productId externally and fetch details by productId (NOT SKU)
    fun setProductId(id: Int) {
        _productId.value = id
        fetchProductDetails(id)
    }

    fun updateKeywordsFromRaw(raw: String) {
        keywords.value = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun fetchProductDetails(productId: Int) {
        _uiState.value = EditDeleteProductUiState.Loading
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            _uiState.value = EditDeleteProductUiState.Error("User ID missing")
            return
        }

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("product_id", productId)  // <-- backend expects product_id now
            }
            val req = Request.Builder()
                .url("$BASE_URL/products/get_by_id")
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
                            title.value = json.optString("title", "")
                            price.value = json.optDouble("price", 0.0).toString()
                            description.value = json.optString("description", "")
                            sku.value = json.optString("sku", "")
                            _imageUrl.value = json.optString("image_url", null)

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
        val idVal = productId.value
        val priceNumber = price.value.toDoubleOrNull()

        if (userId == -1) {
            _uiState.value = EditDeleteProductUiState.Error("User ID missing!")
            return
        }
        if (idVal == null || title.value.isBlank() || priceNumber == null || priceNumber <= 0.0) {
            _uiState.value = EditDeleteProductUiState.Error("Valid product ID, title & price required")
            return
        }

        _uiState.value = EditDeleteProductUiState.SubmittingEdit

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("product_id", idVal)  // use productId here for identification
                put("title", title.value.trim())
                put("price", "%.2f".format(priceNumber))
                put("sku", sku.value.trim().ifBlank { JSONObject.NULL })  // just a field
                put("description", description.value.trim().ifBlank { JSONObject.NULL })
                put("keywords", JSONArray(keywords.value))
                put("image_url", imageUrl.value ?: JSONObject.NULL)
            }
            val req = Request.Builder()
                .url("$BASE_URL/products/edit_by_id")
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
        val idVal = productId.value

        if (userId == -1) {
            _uiState.value = EditDeleteProductUiState.Error("User ID missing")
            return
        }
        if (idVal == null) {
            _uiState.value = EditDeleteProductUiState.Error("Product ID missing")
            return
        }

        _uiState.value = EditDeleteProductUiState.SubmittingDelete

        viewModelScope.launch {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("product_id", idVal)
            }
            val req = Request.Builder()
                .url("$BASE_URL/products/delete_by_id")
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

     fun uploadImage(uri: Uri) {
         viewModelScope.launch {
             val context = getApplication<Application>().applicationContext
             val file = uriToFile(context, uri)
             if (file == null) {
                 _uiState.value = EditDeleteProductUiState.Error("Image conversion failed")
                 return@launch
             }

             uploadImage(file)
         }
     }




     fun uploadImage(file: File, retryCount: Int = 1) {
         _uiState.value = EditDeleteProductUiState.UploadingImage

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
                     _uiState.value = EditDeleteProductUiState.Error("Image upload failed: ${e.message}")
                 }
             }

             override fun onResponse(call: Call, response: Response) {
                 val bodyStr = response.body?.string()
                 if (!response.isSuccessful || bodyStr == null) {
                     _uiState.value = EditDeleteProductUiState.Error("Image upload failed: ${response.message}")
                     return
                 }
                 try {
                     val json = JSONObject(bodyStr)
                     val url = json.optString("image_url", null)
                     if (url == null) {
                         _uiState.value = EditDeleteProductUiState.Error("Image upload failed: No URL returned")
                         return
                     }
                     _imageUrl.value = url
                     _uiState.value = EditDeleteProductUiState.Idle
                 } catch (ex: Exception) {
                     _uiState.value = EditDeleteProductUiState.Error("Image upload failed: ${ex.message}")
                 }
             }
         })
     }

     fun uriToFile(context: Context, uri: Uri): File? {
         return try {
             val inputStream = context.contentResolver.openInputStream(uri) ?: return null
             val tempFile = File.createTempFile("upload", null, context.cacheDir)
             tempFile.outputStream().use { outputStream ->
                 inputStream.copyTo(outputStream)
             }
             tempFile
         } catch (e: Exception) {
             null
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


 }
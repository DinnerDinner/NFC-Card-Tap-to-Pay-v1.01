package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

data class ProductData(
    val id: Int,
    val title: String,
    val price: Double,
    val sku: String?,
    val description: String?,
    val keywords: List<String>,
    val imageUrl: String?
)

sealed class ScanQrState {
    object Idle : ScanQrState()
    object Detecting : ScanQrState()
    data class Detected(val barcode: String) : ScanQrState()
    data class BackendVerifying(val barcode: String) : ScanQrState()
    data class ScanSuccess(val message: String, val product: ProductData) : ScanQrState()
    data class Error(val message: String) : ScanQrState()
    object RedirectReady : ScanQrState()
}

class ScanQrViewModel(private val appContext: Context) : ViewModel() {

    private val _state = MutableStateFlow<ScanQrState>(ScanQrState.Idle)
    val state: StateFlow<ScanQrState> = _state.asStateFlow()

    private val hasScanned = AtomicBoolean(false)
    private val client = OkHttpClient()

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13,
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128,
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A,
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E,
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    private var debounceJob: Job? = null

    fun startScan() {
        if (_state.value == ScanQrState.Idle) {
            _state.value = ScanQrState.Detecting
            hasScanned.set(false)
        }
    }

    fun resetScan() {
        // Resets scan state so another scan can occur
        _state.value = ScanQrState.Idle
        hasScanned.set(false)
    }

    @androidx.camera.core.ExperimentalGetImage
    fun getImageAnalyzer(onValidScan: (String) -> Unit): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            processImageProxy(imageProxy, onValidScan)
        }
    }

    /**
     * Call this from the camera analyzer when a barcode is detected.
     */
    fun onBarcodeDetected(barcodeValue: String) {
        if (hasScanned.compareAndSet(false, true)) {
            _state.value = ScanQrState.Detected(barcodeValue)

            // Cancel any existing debounceJob and start a new one
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                delay(700L) // debounce time to avoid duplicate calls

                verifyBarcodeWithBackend(
                    barcode = barcodeValue,
                    onSuccess = {
                        _state.value = ScanQrState.RedirectReady
                    },
                    onError = { errorMsg ->
                        _state.value = ScanQrState.Error(errorMsg)
                        hasScanned.set(false)
                    }
                )
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy, onValidScan: (String) -> Unit) {
        if (hasScanned.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcodeValue = barcodes.firstOrNull()?.rawValue
                    if (!barcodeValue.isNullOrBlank()) {
                        onValidScan(barcodeValue)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScanQrViewModel", "Barcode scan failure", e)
                    _state.value = ScanQrState.Error("Failed to scan. Try again.")
                    hasScanned.set(false)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun verifyBarcodeWithBackend(
        barcode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _state.value = ScanQrState.BackendVerifying(barcode)

        // Get user_id from SharedPreferences
        val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            onError("User ID not found in preferences.")
            hasScanned.set(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                    put("detected_code", barcode)
                }
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://nfc-fastapi-backend.onrender.com/products/scan")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errMsg = "Server error: ${response.code}"
                        withContext(Dispatchers.Main) {
                            onError(errMsg)
                            hasScanned.set(false)
                        }
                        return@use
                    }

                    val bodyStr = response.body?.string()
                    if (bodyStr.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            onError("Empty response from server")
                            hasScanned.set(false)
                        }
                        return@use
                    }

                    val jsonResponse = JSONObject(bodyStr)
                    val message = jsonResponse.optString("message", "Scan successful")
                    val productJson = jsonResponse.optJSONObject("product")

                    if (productJson == null) {
                        withContext(Dispatchers.Main) {
                            onError("No product data in response")
                            hasScanned.set(false)
                        }
                        return@use
                    }

                    val product = ProductData(
                        id = productJson.optInt("id", -1),
                        title = productJson.optString("title", "Unknown"),
                        price = productJson.optDouble("price", 0.0),
                        sku = productJson.optString("sku", null),
                        description = productJson.optString("description", null),
                        keywords = productJson.optJSONArray("keywords")?.let { arr ->
                            List(arr.length()) { i -> arr.getString(i) }
                        } ?: emptyList(),
                        imageUrl = productJson.optString("image_url", null)
                    )

                    withContext(Dispatchers.Main) {
                        _state.value = ScanQrState.ScanSuccess(message, product)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Unexpected error: ${e.localizedMessage}")
                    hasScanned.set(false)
                }
            }
        }
    }
}















//package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
//
//import android.util.Log
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.mlkit.vision.barcode.common.Barcode
//import com.google.mlkit.vision.barcode.BarcodeScannerOptions
//import com.google.mlkit.vision.barcode.BarcodeScanning
//import com.google.mlkit.vision.common.InputImage
//import kotlinx.coroutines.*
//import kotlinx.coroutines.flow.*
//import java.util.concurrent.atomic.AtomicBoolean
//import android.content.Context
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//
//data class ProductData(
//    val id: Int,
//    val title: String,
//    val price: Double,
//    val sku: String?,
//    val description: String?,
//    val keywords: List<String>,
//    val imageUrl: String?
//)
//
//sealed class ScanQrState {
//    object Idle : ScanQrState()
//    object Detecting : ScanQrState()
//    data class Detected(val barcode: String) : ScanQrState()
//    data class BackendVerifying(val barcode: String) : ScanQrState()
//    data class ScanSuccess(val message: String, val product: ProductData) : ScanQrState()
//    data class Error(val message: String) : ScanQrState()
//    object RedirectReady : ScanQrState()
//}
//
//class ScanQrViewModel(private val appContext: Context) : ViewModel() {
//
//    private val _state = MutableStateFlow<ScanQrState>(ScanQrState.Idle)
//    val state: StateFlow<ScanQrState> = _state.asStateFlow()
//
//    private val hasScanned = AtomicBoolean(false)
//    private val client = OkHttpClient()
//
//    private val scanner = BarcodeScanning.getClient(
//        BarcodeScannerOptions.Builder()
//            .setBarcodeFormats(
//                Barcode.FORMAT_EAN_13,
//                Barcode.FORMAT_CODE_128,
//                Barcode.FORMAT_UPC_A,
//                Barcode.FORMAT_UPC_E,
//                Barcode.FORMAT_QR_CODE
//            )
//            .build()
//    )
//
//    private var debounceJob: Job? = null
//
//
//
//    fun startScan() {
//        if (_state.value == ScanQrState.Idle) {
//            _state.value = ScanQrState.Detecting
//        }
//    }
//
//    @androidx.camera.core.ExperimentalGetImage
//    fun getImageAnalyzer(onValidScan: (String) -> Unit): ImageAnalysis.Analyzer {
//        return ImageAnalysis.Analyzer { imageProxy ->
//            processImageProxy(imageProxy, onValidScan)
//        }
//    }
////    fun resetAfterRedirect() {
////        _state.value = ScanQrState.Idle
////        hasScanned.set(false)
////    }
//
//
//    @androidx.camera.core.ExperimentalGetImage
//    private fun processImageProxy(imageProxy: ImageProxy, onValidScan: (String) -> Unit) {
//        if (hasScanned.get()) {
//            imageProxy.close()
//            return
//        }
//
//        val mediaImage = imageProxy.image
//        if (mediaImage != null) {
//            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//
//            scanner.process(image)
//                .addOnSuccessListener { barcodes ->
//                    val barcodeValue = barcodes.firstOrNull()?.rawValue
//                    if (!barcodeValue.isNullOrBlank()) {
//                        if (hasScanned.compareAndSet(false, true)) {
//                            _state.value = ScanQrState.Detected(barcodeValue)
//                            debounceJob = viewModelScope.launch {
//                                delay(700L)
//                                verifyBarcodeWithBackend(
//                                    barcode = barcodeValue,
//                                    onSuccess = {
//                                        _state.value = ScanQrState.RedirectReady
//                                        onValidScan(barcodeValue)
//                                    },
//                                    onError = { error ->
//                                        _state.value = ScanQrState.Error(error)
//                                        hasScanned.set(false)
//                                    }
//                                )
//                            }
//                        }
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ScanQrViewModel", "Barcode scan failure", e)
//                    _state.value = ScanQrState.Error("Failed to scan. Try again.")
//                    hasScanned.set(false)
//                }
//                .addOnCompleteListener {
//                    imageProxy.close()
//                }
//        } else {
//            imageProxy.close()
//        }
//    }
//
//    private fun verifyBarcodeWithBackend(
//        barcode: String,
//        onSuccess: () -> Unit,
//        onError: (String) -> Unit
//    ) {
//        _state.value = ScanQrState.BackendVerifying(barcode)
//
//        // Get user_id and business_id from shared preferences
//        val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        val userId = prefs.getInt("user_id", -1)
////        val businessId = prefs.getInt("business_id", -1)
//
//        if (userId == -1) {
//            onError("User or business ID not found in preferences.")
//            hasScanned.set(false)
//            return
//        }
//
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val jsonBody = JSONObject().apply {
//                    put("user_id", userId)
////                    put("business_id", businessId)
//                    put("detected_code", barcode)
//                }
//                val mediaType = "application/json; charset=utf-8".toMediaType()
//                val requestBody = jsonBody.toString().toRequestBody(mediaType)
//
//                val request = Request.Builder()
//                    .url("https://nfc-fastapi-backend.onrender.com/products/scan")
//                    .post(requestBody)
//                    .build()
//
//                client.newCall(request).execute().use { response ->
//                    if (!response.isSuccessful) {
//                        val errMsg = "Server error: ${response.code}"
//                        withContext(Dispatchers.Main) {
//                            onError(errMsg)
//                            hasScanned.set(false)
//                        }
//                        return@use
//                    }
//
//                    val bodyStr = response.body?.string()
//                    if (bodyStr.isNullOrEmpty()) {
//                        withContext(Dispatchers.Main) {
//                            onError("Empty response from server")
//                            hasScanned.set(false)
//                        }
//                        return@use
//                    }
//
//                    val jsonResponse = JSONObject(bodyStr)
//                    val message = jsonResponse.optString("message", "Scan successful")
//                    val productJson = jsonResponse.optJSONObject("product")
//
//                    if (productJson == null) {
//                        withContext(Dispatchers.Main) {
//                            onError("No product data in response")
//                            hasScanned.set(false)
//                        }
//                        return@use
//                    }
//
//                    val product = ProductData(
//                        id = productJson.optInt("id", -1),
//                        title = productJson.optString("title", "Unknown"),
//                        price = productJson.optDouble("price", 0.0),
//                        sku = productJson.optString("sku", null),
//                        description = productJson.optString("description", null),
//                        keywords = productJson.optJSONArray("keywords")?.let { arr ->
//                            List(arr.length()) { i -> arr.getString(i) }
//                        } ?: emptyList(),
//                        imageUrl = productJson.optString("image_url", null)
//                    )
//
//                    withContext(Dispatchers.Main) {
//                        _state.value = ScanQrState.ScanSuccess(message, product)
//                        onSuccess()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    onError("Unexpected error: ${e.localizedMessage}")
//                    hasScanned.set(false)
//                }
//            }
//        }
//    }
//}
//
//
//
////package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
////
////import android.util.Log
////import androidx.camera.core.ImageAnalysis
////import androidx.camera.core.ImageProxy
////import androidx.lifecycle.ViewModel
////import androidx.lifecycle.viewModelScope
////import com.google.mlkit.vision.barcode.common.Barcode
////import com.google.mlkit.vision.barcode.BarcodeScannerOptions
////import com.google.mlkit.vision.barcode.BarcodeScanning
////import com.google.mlkit.vision.common.InputImage
////import kotlinx.coroutines.*
////import kotlinx.coroutines.flow.*
////import retrofit2.HttpException
////import retrofit2.Retrofit
////import retrofit2.converter.gson.GsonConverterFactory
////import retrofit2.http.Body
////import retrofit2.http.POST
////import java.util.concurrent.atomic.AtomicBoolean
////import android.content.Context
////import okhttp3.MediaType.Companion.toMediaType
////import okhttp3.OkHttpClient
////import okhttp3.Request
////import okhttp3.RequestBody.Companion.toRequestBody
////import org.json.JSONObject
////
////// Add this data class inside ViewModel file (or external file)
////data class ProductData(
////    val id: Int,
////    val title: String,
////    val price: Double,
////    val sku: String?,
////    val description: String?,
////    val keywords: List<String>,
////    val imageUrl: String?
////)
////
////// Add a new state to ScanQrState sealed class for success with message + product
////sealed class ScanQrState {
////    object Idle : ScanQrState()
////    object Detecting : ScanQrState()
////    data class Detected(val barcode: String) : ScanQrState()
////    data class BackendVerifying(val barcode: String) : ScanQrState()
////    data class ScanSuccess(val message: String, val product: ProductData) : ScanQrState()  // NEW
////    data class Error(val message: String) : ScanQrState()
////    object RedirectReady : ScanQrState()
////}
////
////
////
////data class ScanRequest(
////    val user_id: Int,
////    val business_id: Int,
////    val detected_code: String
////)
////
////data class ScanResponse(
////    val message: String
////)
////
////interface ScanApi {
////    @POST("/products/scan")
////    suspend fun scanProduct(@Body request: ScanRequest): ScanResponse
////}
////
////class ScanQrViewModel : ViewModel() {
////
////    private val _state = MutableStateFlow<ScanQrState>(ScanQrState.Idle)
////    val state: StateFlow<ScanQrState> = _state.asStateFlow()
////
////    private val hasScanned = AtomicBoolean(false)
////
////    private val client = OkHttpClient()
////
////
////    private val scanner = BarcodeScanning.getClient(
////        BarcodeScannerOptions.Builder()
////            .setBarcodeFormats(
////                Barcode.FORMAT_EAN_13,
////                Barcode.FORMAT_CODE_128,
////                Barcode.FORMAT_UPC_A,
////                Barcode.FORMAT_UPC_E,
////                Barcode.FORMAT_QR_CODE
////            )
////            .build()
////    )
////
////    private var debounceJob: Job? = null
////
////    private val retrofit = Retrofit.Builder()
////        .baseUrl("https://nfc-fastapi-backend.onrender.com")
////        .addConverterFactory(GsonConverterFactory.create())
////        .build()
////
////
////
////    private val scanApi = retrofit.create(ScanApi::class.java)
////
////    private fun verifyBarcodeWithBackend(
////        barcode: String,
////        onSuccess: () -> Unit,
////        onError: (String) -> Unit
////    ) {
////        _state.value = ScanQrState.BackendVerifying(barcode)
////
////        // Pull user_id and business_id from shared prefs (adjust prefs keys accordingly)
////        val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
////        val userId = prefs.getInt("user_id", -1)
////        val businessId = prefs.getInt("business_id", -1)
////
////        if (userId == -1 || businessId == -1) {
////            onError("User or business ID not found in preferences.")
////            hasScanned.set(false)
////            return
////        }
////
////        viewModelScope.launch(Dispatchers.IO) {
////            try {
////                val jsonBody = JSONObject().apply {
////                    put("user_id", userId)
////                    put("business_id", businessId)
////                    put("detected_code", barcode)
////                }
////                val mediaType = "application/json; charset=utf-8".toMediaType()
////                val requestBody = jsonBody.toString().toRequestBody(mediaType)
////
////                val request = Request.Builder()
////                    .url("https://nfc-fastapi-backend.onrender.com/products/scan")
////                    .post(requestBody)
////                    .build()
////
////                client.newCall(request).execute().use { response ->
////                    if (!response.isSuccessful) {
////                        val errMsg = "Server error: ${response.code}"
////                        withContext(Dispatchers.Main) {
////                            onError(errMsg)
////                            hasScanned.set(false)
////                        }
////                        return@use
////                    }
////
////                    val bodyStr = response.body?.string()
////                    if (bodyStr.isNullOrEmpty()) {
////                        withContext(Dispatchers.Main) {
////                            onError("Empty response from server")
////                            hasScanned.set(false)
////                        }
////                        return@use
////                    }
////
////                    val jsonResponse = JSONObject(bodyStr)
////                    val message = jsonResponse.optString("message", "Scan successful")
////                    val productJson = jsonResponse.optJSONObject("product")
////
////                    if (productJson == null) {
////                        withContext(Dispatchers.Main) {
////                            onError("No product data in response")
////                            hasScanned.set(false)
////                        }
////                        return@use
////                    }
////
////                    val product = ProductData(
////                        id = productJson.optInt("id", -1),
////                        title = productJson.optString("title", "Unknown"),
////                        price = productJson.optDouble("price", 0.0),
////                        sku = productJson.optString("sku", null),
////                        description = productJson.optString("description", null),
////                        keywords = productJson.optJSONArray("keywords")?.let { arr ->
////                            List(arr.length()) { i -> arr.getString(i) }
////                        } ?: emptyList(),
////                        imageUrl = productJson.optString("image_url", null)
////                    )
////
////                    withContext(Dispatchers.Main) {
////                        _state.value = ScanQrState.ScanSuccess(message, product)
////                        onSuccess()
////                    }
////                }
////            } catch (e: Exception) {
////                withContext(Dispatchers.Main) {
////                    onError("Unexpected error: ${e.localizedMessage}")
////                    hasScanned.set(false)
////                }
////            }
////        }
////    }
////
////    fun resetScan() {
////        _state.value = ScanQrState.Detecting
////        hasScanned.set(false)
////        debounceJob?.cancel()
////    }
////
////    fun startScan() {
////        if (_state.value == ScanQrState.Idle) {
////            _state.value = ScanQrState.Detecting
////        }
////    }
////
////    @androidx.camera.core.ExperimentalGetImage
////    fun getImageAnalyzer(onValidScan: (String) -> Unit): ImageAnalysis.Analyzer {
////        return ImageAnalysis.Analyzer { imageProxy ->
////            processImageProxy(imageProxy, onValidScan)
////        }
////    }
////
////    @androidx.camera.core.ExperimentalGetImage
////    private fun processImageProxy(imageProxy: ImageProxy, onValidScan: (String) -> Unit) {
////        if (hasScanned.get()) {
////            imageProxy.close()
////            return
////        }
////
////        val mediaImage = imageProxy.image
////        if (mediaImage != null) {
////            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
////
////            scanner.process(image)
////                .addOnSuccessListener { barcodes ->
////                    val barcodeValue = barcodes.firstOrNull()?.rawValue
////                    if (!barcodeValue.isNullOrBlank()) {
////                        if (hasScanned.compareAndSet(false, true)) {
////                            _state.value = ScanQrState.Detected(barcodeValue)
////                            debounceJob = viewModelScope.launch {
////                                delay(700L)
////                                verifyBarcodeWithBackend(
////                                    barcode = barcodeValue,
////                                    onSuccess = { message ->
////                                        _state.value = ScanQrState.RedirectReady
////                                        onValidScan(message)
////                                    },
////                                    onError = { error ->
////                                        _state.value = ScanQrState.Error(error)
////                                        hasScanned.set(false)
////                                    }
////                                )
////                            }
////                        }
////                    }
////                }
////                .addOnFailureListener { e ->
////                    Log.e("ScanQrViewModel", "Barcode scan failure", e)
////                    _state.value = ScanQrState.Error("Failed to scan. Try again.")
////                    hasScanned.set(false)
////                }
////                .addOnCompleteListener {
////                    imageProxy.close()
////                }
////        } else {
////            imageProxy.close()
////        }
////    }
////
////    private fun verifyBarcodeWithBackend(
////        barcode: String,
////        onSuccess: (String) -> Unit,
////        onError: (String) -> Unit
////    ) {
////        _state.value = ScanQrState.BackendVerifying(barcode)
////        viewModelScope.launch {
////            try {
////                val request = ScanRequest(
////                    user_id = userId,
////                    business_id = businessId,
////                    detected_code = barcode
////                )
////                val response = scanApi.scanProduct(request)
////                onSuccess(response.message)
////            } catch (e: HttpException) {
////                Log.e("ScanQrViewModel", "HTTP Error: ${e.code()}")
////                onError("Server error (${e.code()})")
////            } catch (e: Exception) {
////                Log.e("ScanQrViewModel", "Unexpected Error", e)
////                onError("Unexpected error occurred.")
////            }
////        }
////    }
////}
////
////
////
////
////
////
////
////
////
//////
//////package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
//////
//////import android.util.Log
//////import androidx.camera.core.ImageAnalysis
//////import androidx.camera.core.ImageProxy
//////import androidx.lifecycle.ViewModel
//////import androidx.lifecycle.viewModelScope
//////import com.google.mlkit.vision.barcode.common.Barcode
//////
//////import retrofit2.HttpException
//////
//////import com.google.mlkit.vision.barcode.BarcodeScannerOptions
//////import com.google.mlkit.vision.barcode.BarcodeScanning
//////import com.google.mlkit.vision.common.InputImage
//////import kotlinx.coroutines.*
//////import kotlinx.coroutines.flow.*
//////import java.util.concurrent.atomic.AtomicBoolean
//////
//////sealed class ScanQrState {
//////    object Idle : ScanQrState()
//////    object Detecting : ScanQrState()
//////    data class Detected(val barcode: String) : ScanQrState()
//////    data class BackendVerifying(val barcode: String) : ScanQrState()
//////    data class Error(val message: String) : ScanQrState()
//////    object RedirectReady : ScanQrState()
//////}
//////
//////class ScanQrViewModel : ViewModel() {
//////
//////    private val _state = MutableStateFlow<ScanQrState>(ScanQrState.Idle)
//////    val state: StateFlow<ScanQrState> = _state.asStateFlow()
//////
//////    // Prevent multiple scans or overlapping requests
//////    private val hasScanned = AtomicBoolean(false)
//////
//////    private val scanner = BarcodeScanning.getClient(
//////        BarcodeScannerOptions.Builder()
//////            .setBarcodeFormats(
//////                Barcode.FORMAT_EAN_13,
//////                Barcode.FORMAT_CODE_128,
//////                Barcode.FORMAT_UPC_A,
//////                Barcode.FORMAT_UPC_E,
//////                Barcode.FORMAT_QR_CODE
//////            )
//////            .build()
//////    )
//////
//////    // Debounce control
//////    private var debounceJob: Job? = null
//////
//////    fun resetScan() {
//////        _state.value = ScanQrState.Detecting
//////        hasScanned.set(false)
//////        debounceJob?.cancel()
//////    }
//////
//////    fun startScan() {
//////        if (_state.value == ScanQrState.Idle) {
//////            _state.value = ScanQrState.Detecting
//////        }
//////    }
//////    @androidx.camera.core.ExperimentalGetImage
//////    fun getImageAnalyzer(onValidScan: (String) -> Unit): ImageAnalysis.Analyzer {
//////        return ImageAnalysis.Analyzer { imageProxy ->
//////            processImageProxy(imageProxy, onValidScan)
//////        }
//////    }
//////    @androidx.camera.core.ExperimentalGetImage
//////    private fun processImageProxy(imageProxy: ImageProxy, onValidScan: (String) -> Unit) {
//////        if (hasScanned.get()) {
//////            imageProxy.close()
//////            return
//////        }
//////
//////        val mediaImage = imageProxy.image
//////        if (mediaImage != null) {
//////            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//////
//////            scanner.process(image)
//////                .addOnSuccessListener { barcodes ->
//////                    val barcodeValue = barcodes.firstOrNull()?.rawValue
//////                    if (!barcodeValue.isNullOrBlank()) {
//////                        if (hasScanned.compareAndSet(false, true)) {
//////                            _state.value = ScanQrState.Detected(barcodeValue)
//////                            debounceJob = viewModelScope.launch {
//////                                delay(700L) // Give UI time to show freeze/feedback
//////                                verifyBarcodeWithBackend(
//////                                    barcodeValue,
//////                                    onSuccess = {
//////                                        _state.value = ScanQrState.RedirectReady
//////                                        onValidScan(barcodeValue)
//////                                    },
//////                                    onError = { error ->
//////                                        _state.value = ScanQrState.Error(error)
//////                                        hasScanned.set(false)
//////                                    }
//////                                )
//////                            }
//////                        }
//////                    }
//////                }
//////                .addOnFailureListener { e ->
//////                    Log.e("ScanQrViewModel", "Barcode scan failure", e)
//////                    _state.value = ScanQrState.Error("Failed to scan. Try again.")
//////                    hasScanned.set(false)
//////                }
//////                .addOnCompleteListener {
//////                    imageProxy.close()
//////                }
//////        } else {
//////            imageProxy.close()
//////        }
//////    }
//////
//////    private fun verifyBarcodeWithBackend(
//////        barcode: String,
//////        onSuccess: () -> Unit,
//////        onError: (String) -> Unit
//////    ) {
//////        _state.value = ScanQrState.BackendVerifying(barcode)
//////        viewModelScope.launch {
//////            try {
//////                // TODO: Replace with real Retrofit logic
//////                // val response = backendApi.scanBarcode(barcode)
//////                delay(1000) // simulate API call
//////                val success = true // simulate success
//////
//////                if (success) {
//////                    onSuccess()
//////                } else {
//////                    onError("Barcode not found in system.")
//////                }
//////
//////            } catch (e: HttpException) {
//////                Log.e("ScanQrViewModel", "HTTP Error: ${e.code()}")
//////                onError("Server error (${e.code()})")
//////            } catch (e: Exception) {
//////                Log.e("ScanQrViewModel", "Unexpected Error", e)
//////                onError("Unexpected error occurred.")
//////            }
//////        }
//////    }
//////}

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
    data class NavigateToScannedProduct(val product: ProductData, val scannedCode: String) : ScanQrState()
}

class ScanQrViewModel(private val appContext: Context) : ViewModel() {

    private val _state = MutableStateFlow<ScanQrState>(ScanQrState.Idle)
    val state: StateFlow<ScanQrState> = _state.asStateFlow()

    private val hasScanned = AtomicBoolean(false)
    private val client = OkHttpClient()

    // Store the scanned product data and code for passing to next screen
    private var _scannedProduct: ProductData? = null
    private var _scannedCode: String? = null

    val scannedProduct: ProductData? get() = _scannedProduct
    val scannedCode: String? get() = _scannedCode

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
        // Clear stored data when resetting
        _scannedProduct = null
        _scannedCode = null
    }

    fun clearScannedData() {
        // Method to clear the stored data after navigation
        _scannedProduct = null
        _scannedCode = null
    }

    fun triggerNavigationToScannedProduct() {
        // Call this when ready to navigate to the scanned product screen
        val product = _scannedProduct
        val code = _scannedCode
        if (product != null && code != null) {
            _state.value = ScanQrState.NavigateToScannedProduct(product, code)
        }
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
            _scannedCode = barcodeValue // Store the scanned code

            // Cancel any existing debounceJob and start a new one
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch {
                delay(700L) // debounce time to avoid duplicate calls

                verifyBarcodeWithBackend(
                    barcode = barcodeValue,
                    onSuccess = { product ->
                        _scannedProduct = product // Store the product data
                        // Automatically trigger navigation after successful scan
                        _state.value = ScanQrState.NavigateToScannedProduct(product, barcodeValue)
                    },
                    onError = { errorMsg ->
                        _state.value = ScanQrState.Error(errorMsg)
                        hasScanned.set(false)
                        // Clear data on error
                        _scannedProduct = null
                        _scannedCode = null
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
        onSuccess: (ProductData) -> Unit,
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
                        onSuccess(product) // Pass the product to the success callback
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
//import android.content.Context
//import android.util.Log
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.mlkit.vision.barcode.BarcodeScannerOptions
//import com.google.mlkit.vision.barcode.BarcodeScanning
//import com.google.mlkit.vision.common.InputImage
//import kotlinx.coroutines.*
//import kotlinx.coroutines.flow.*
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.util.concurrent.atomic.AtomicBoolean
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
//                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13,
//                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128,
//                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A,
//                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E,
//                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
//            )
//            .build()
//    )
//
//    private var debounceJob: Job? = null
//
//    fun startScan() {
//        if (_state.value == ScanQrState.Idle) {
//            _state.value = ScanQrState.Detecting
//            hasScanned.set(false)
//        }
//    }
//
//    fun resetScan() {
//        // Resets scan state so another scan can occur
//        _state.value = ScanQrState.Idle
//        hasScanned.set(false)
//    }
//
//    @androidx.camera.core.ExperimentalGetImage
//    fun getImageAnalyzer(onValidScan: (String) -> Unit): ImageAnalysis.Analyzer {
//        return ImageAnalysis.Analyzer { imageProxy ->
//            processImageProxy(imageProxy, onValidScan)
//        }
//    }
//
//    /**
//     * Call this from the camera analyzer when a barcode is detected.
//     */
//    fun onBarcodeDetected(barcodeValue: String) {
//        if (hasScanned.compareAndSet(false, true)) {
//            _state.value = ScanQrState.Detected(barcodeValue)
//
//            // Cancel any existing debounceJob and start a new one
//            debounceJob?.cancel()
//            debounceJob = viewModelScope.launch {
//                delay(700L) // debounce time to avoid duplicate calls
//
//                verifyBarcodeWithBackend(
//                    barcode = barcodeValue,
//                    onSuccess = {
//                        _state.value = ScanQrState.RedirectReady
//                    },
//                    onError = { errorMsg ->
//                        _state.value = ScanQrState.Error(errorMsg)
//                        hasScanned.set(false)
//                    }
//                )
//            }
//        }
//    }
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
//                        onValidScan(barcodeValue)
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
//        // Get user_id from SharedPreferences
//        val prefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//        val userId = prefs.getInt("user_id", -1)
//
//        if (userId == -1) {
//            onError("User ID not found in preferences.")
//            hasScanned.set(false)
//            return
//        }
//
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val jsonBody = JSONObject().apply {
//                    put("user_id", userId)
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
//
//
//
//
//
//
//
//
//

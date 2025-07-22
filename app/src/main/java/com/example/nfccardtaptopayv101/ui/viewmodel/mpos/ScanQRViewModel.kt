
package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode

import retrofit2.HttpException

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

sealed class ScanQrState {
    object Idle : ScanQrState()
    object Detecting : ScanQrState()
    data class Detected(val barcode: String) : ScanQrState()
    data class BackendVerifying(val barcode: String) : ScanQrState()
    data class Error(val message: String) : ScanQrState()
    object RedirectReady : ScanQrState()
}

class ScanQrViewModel : ViewModel() {

    private val _state = MutableStateFlow<ScanQrState>(ScanQrState.Idle)
    val state: StateFlow<ScanQrState> = _state.asStateFlow()

    // Prevent multiple scans or overlapping requests
    private val hasScanned = AtomicBoolean(false)

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE
            )
            .build()
    )

    // Debounce control
    private var debounceJob: Job? = null

    fun resetScan() {
        _state.value = ScanQrState.Detecting
        hasScanned.set(false)
        debounceJob?.cancel()
    }

    fun startScan() {
        if (_state.value == ScanQrState.Idle) {
            _state.value = ScanQrState.Detecting
        }
    }
    @androidx.camera.core.ExperimentalGetImage
    fun getImageAnalyzer(onValidScan: (String) -> Unit): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            processImageProxy(imageProxy, onValidScan)
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
                        if (hasScanned.compareAndSet(false, true)) {
                            _state.value = ScanQrState.Detected(barcodeValue)
                            debounceJob = viewModelScope.launch {
                                delay(700L) // Give UI time to show freeze/feedback
                                verifyBarcodeWithBackend(
                                    barcodeValue,
                                    onSuccess = {
                                        _state.value = ScanQrState.RedirectReady
                                        onValidScan(barcodeValue)
                                    },
                                    onError = { error ->
                                        _state.value = ScanQrState.Error(error)
                                        hasScanned.set(false)
                                    }
                                )
                            }
                        }
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
        viewModelScope.launch {
            try {
                // TODO: Replace with real Retrofit logic
                // val response = backendApi.scanBarcode(barcode)
                delay(1000) // simulate API call
                val success = true // simulate success

                if (success) {
                    onSuccess()
                } else {
                    onError("Barcode not found in system.")
                }

            } catch (e: HttpException) {
                Log.e("ScanQrViewModel", "HTTP Error: ${e.code()}")
                onError("Server error (${e.code()})")
            } catch (e: Exception) {
                Log.e("ScanQrViewModel", "Unexpected Error", e)
                onError("Unexpected error occurred.")
            }
        }
    }
}

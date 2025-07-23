package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ScannedProductState {
    object Loading : ScannedProductState()
    data class DisplayProduct(
        val product: ProductData,
        val scannedCode: String
    ) : ScannedProductState()
    data class Error(val message: String) : ScannedProductState()
}

class ScannedProductViewModel(
    private val appContext: Context,
    private val initialProduct: ProductData,
    private val initialScannedCode: String
) : ViewModel() {

    private val _state = MutableStateFlow<ScannedProductState>(ScannedProductState.Loading)
    val state: StateFlow<ScannedProductState> = _state.asStateFlow()

    // Store the product ID for later use (as requested)
    var currentProductId: Int = initialProduct.id
        private set

    init {
        // Initialize with the product data from scan
        _state.value = ScannedProductState.DisplayProduct(
            product = initialProduct,
            scannedCode = initialScannedCode
        )
        currentProductId = initialProduct.id
    }

    fun confirmAndContinueScanning(): Boolean {
        // Logic for when user wants to continue scanning more products
        // You can add any validation or business logic here
        return true
    }

    fun confirmAndGoToSalesPage(): Boolean {
        // Logic for when user wants to go to sales page
        // You can add any validation or business logic here
        // Later you'll use currentProductId to add to cart/sales page
        return true
    }

    fun getProductData(): ProductData? {
        return when (val currentState = _state.value) {
            is ScannedProductState.DisplayProduct -> currentState.product
            else -> null
        }
    }

    fun getScannedCode(): String? {
        return when (val currentState = _state.value) {
            is ScannedProductState.DisplayProduct -> currentState.scannedCode
            else -> null
        }
    }

    // Helper method to format price for display
    fun getFormattedPrice(): String {
        return getProductData()?.let { product ->
            "$%.2f".format(product.price)
        } ?: "$0.00"
    }

    // Helper method to get display title (fallback if empty)
    fun getDisplayTitle(): String {
        return getProductData()?.title?.takeIf { it.isNotBlank() } ?: "Unknown Product"
    }

    // Helper method to get display SKU
    fun getDisplaySku(): String {
        return getProductData()?.sku ?: "No SKU"
    }

    // Helper method to get image URL
    fun getImageUrl(): String? {
        return getProductData()?.imageUrl
    }

    // Helper method for debugging or logging
    fun logProductInfo() {
        val product = getProductData()
        val code = getScannedCode()
        android.util.Log.d("ScannedProductVM",
            "Product: ${product?.title}, ID: $currentProductId, Code: $code"
        )
    }
}
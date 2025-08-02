package com.example.nfccardtaptopayv101.ui.viewmodel.ble

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.util.*

private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"

sealed class PaymentRequestState {
    object Idle : PaymentRequestState()
    object Processing : PaymentRequestState()
    object Success : PaymentRequestState()
    data class Error(val message: String) : PaymentRequestState()
}

data class Screen4UiState(
    val selectedUser: NearbyUser? = null,
    val currentAmount: String = "0.00",
    val isValidAmount: Boolean = false,
    val paymentRequestState: PaymentRequestState = PaymentRequestState.Idle,
    val merchantUserId: String = "",
    val statusMessage: String = ""
)

class Screen4ViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "Screen4ViewModel"
        private const val MAX_AMOUNT = 9999.99
        private const val MIN_AMOUNT = 0.01
    }

    private val context = getApplication<Application>()

    // HTTP client for API calls
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _uiState = MutableStateFlow(Screen4UiState())
    val uiState: StateFlow<Screen4UiState> = _uiState.asStateFlow()

    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun setSelectedUser(user: NearbyUser) {
        Log.d(TAG, "Setting selected user: ${user.userId} - ${user.userName}")
        _uiState.value = _uiState.value.copy(
            selectedUser = user,
            statusMessage = "Enter amount to request from ${user.userName.ifEmpty { "User ${user.userId}" }}"
        )
    }

    fun setMerchantUserId(userId: String) {
        Log.d(TAG, "Setting merchant user ID: $userId")
        _uiState.value = _uiState.value.copy(merchantUserId = userId)
    }

    fun onNumberPressed(digit: String) {
        val currentAmount = _uiState.value.currentAmount
        val newAmount = when (digit) {
            "." -> {
                if (!currentAmount.contains(".")) {
                    if (currentAmount == "0.00") "0." else "$currentAmount."
                } else currentAmount
            }
            else -> {
                if (currentAmount == "0.00") {
                    digit
                } else if (currentAmount.contains(".") && currentAmount.substringAfter(".").length >= 2) {
                    currentAmount // Don't add more than 2 decimal places
                } else {
                    currentAmount + digit
                }
            }
        }

        val isValid = isValidAmount(newAmount)

        _uiState.value = _uiState.value.copy(
            currentAmount = newAmount,
            isValidAmount = isValid
        )
    }

    fun onBackspacePressed() {
        val currentAmount = _uiState.value.currentAmount
        val newAmount = if (currentAmount.length > 1) {
            currentAmount.dropLast(1)
        } else {
            "0.00"
        }

        val isValid = isValidAmount(newAmount)

        _uiState.value = _uiState.value.copy(
            currentAmount = newAmount,
            isValidAmount = isValid
        )
    }

    fun onClearPressed() {
        _uiState.value = _uiState.value.copy(
            currentAmount = "0.00",
            isValidAmount = false
        )
    }

    private fun isValidAmount(amount: String): Boolean {
        return try {
            val value = amount.toDoubleOrNull() ?: return false
            value >= MIN_AMOUNT && value <= MAX_AMOUNT
        } catch (e: Exception) {
            false
        }
    }

    fun sendPaymentRequest() {
        val currentState = _uiState.value
        val selectedUser = currentState.selectedUser
        val merchantId = currentState.merchantUserId
        val amount = currentState.currentAmount

        if (selectedUser == null) {
            Log.e(TAG, "No user selected")
            _uiState.value = _uiState.value.copy(
                paymentRequestState = PaymentRequestState.Error("No user selected"),
                statusMessage = "Error: No user selected"
            )
            return
        }

        if (merchantId.isEmpty()) {
            Log.e(TAG, "Merchant ID not set")
            _uiState.value = _uiState.value.copy(
                paymentRequestState = PaymentRequestState.Error("Merchant ID not set"),
                statusMessage = "Error: Merchant ID not available"
            )
            return
        }

        if (!isValidAmount(amount)) {
            Log.e(TAG, "Invalid amount: $amount")
            _uiState.value = _uiState.value.copy(
                paymentRequestState = PaymentRequestState.Error("Invalid amount"),
                statusMessage = "Please enter a valid amount"
            )
            return
        }

        Log.d(TAG, "Sending payment request - Merchant: $merchantId, Customer: ${selectedUser.userId}, Amount: $amount")

        _uiState.value = _uiState.value.copy(
            paymentRequestState = PaymentRequestState.Processing,
            statusMessage = "Sending payment request..."
        )

        viewModelScope.launch {
            try {
                val success = sendPaymentRequestToBackend(
                    merchantId = merchantId,
                    customerId = selectedUser.userId,
                    amount = amount.toDouble()
                )

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        paymentRequestState = PaymentRequestState.Success,
                        statusMessage = "Payment request sent successfully!"
                    )
                    Log.d(TAG, "Payment request sent successfully")
                } else {
                    _uiState.value = _uiState.value.copy(
                        paymentRequestState = PaymentRequestState.Error("Failed to send request"),
                        statusMessage = "Failed to send payment request. Please try again."
                    )
                    Log.e(TAG, "Failed to send payment request")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception sending payment request", e)
                _uiState.value = _uiState.value.copy(
                    paymentRequestState = PaymentRequestState.Error(e.message ?: "Unknown error"),
                    statusMessage = "Error sending request: ${e.message}"
                )
            }
        }
    }

    private suspend fun sendPaymentRequestToBackend(
        merchantId: String,
        customerId: String,
        amount: Double
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("merchant_user_id", merchantId.toIntOrNull() ?: return@withContext false)
                    put("customer_user_id", customerId.toIntOrNull() ?: return@withContext false)
                    put("amount", amount)
                }

                val mediaType = "application/json".toMediaType()
                val requestBody = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("$BASE_URL/payment/request")
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Sending payment request: ${json.toString()}")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Payment request response: ${response.code}, body: $responseBody")

                    if (response.isSuccessful) {
                        true
                    } else {
                        Log.e(TAG, "Payment request failed: ${response.code} - $responseBody")
                        false
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error sending payment request", e)
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error sending payment request", e)
                false
            }
        }
    }

    fun getFormattedAmount(): String {
        val amount = _uiState.value.currentAmount.toDoubleOrNull() ?: 0.0
        return numberFormat.format(amount)
    }

    fun resetState() {
        _uiState.value = Screen4UiState()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}
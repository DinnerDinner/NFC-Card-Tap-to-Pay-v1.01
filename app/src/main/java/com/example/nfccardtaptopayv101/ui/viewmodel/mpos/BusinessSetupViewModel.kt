package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private const val BASE_URL = "https://promoted-quetzal-visually.ngrok-free.app"

sealed class BusinessUiState {
    object Idle : BusinessUiState()
    object Loading : BusinessUiState()
    object ShowSetupForm : BusinessUiState()
    data class ShowDashboard(val businessName: String) : BusinessUiState()
    data class Error(val msg: String) : BusinessUiState()
}

class BusinessViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<BusinessUiState>(BusinessUiState.Idle)
    val uiState: StateFlow<BusinessUiState> = _uiState

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    private fun getUserId(): Int = prefs.getInt("user_id", -1)

    fun checkBusinessExists() {
        val userId = getUserId()
        if (userId == -1) {
            _uiState.value = BusinessUiState.Error("User ID not found")
            return
        }

        _uiState.value = BusinessUiState.Loading

        viewModelScope.launch {
            val body = JSONObject().put("user_id", userId)
                .toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$BASE_URL/business/exists")
                .post(body)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = BusinessUiState.Error("Network error")
                }

                override fun onResponse(call: Call, res: Response) {
                    val raw = res.body?.string() ?: "{}"
                    val json = JSONObject(raw)
                    val exists = json.optBoolean("has_business", false)
                    val name = json.optString("business_name", "")
                    _uiState.value = if (exists)
                        BusinessUiState.ShowDashboard(name)
                    else
                        BusinessUiState.ShowSetupForm
                }
            })
        }
    }

    fun submitBusiness(businessName: String) {
        val userId = getUserId()
        if (userId == -1) {
            _uiState.value = BusinessUiState.Error("User ID not found")
            return
        }

        _uiState.value = BusinessUiState.Loading

        viewModelScope.launch {
            val json = JSONObject().apply {
                put("user_id", userId)
                put("business_name", businessName)
            }

            val req = Request.Builder()
                .url("$BASE_URL/business/setup")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _uiState.value = BusinessUiState.Error("Network error")
                }

                override fun onResponse(call: Call, res: Response) {
                    if (res.isSuccessful) {
                        _uiState.value = BusinessUiState.ShowDashboard(businessName)
                    } else {
                        _uiState.value = BusinessUiState.Error("Server error: ${res.message}")
                    }
                }
            })
        }
    }

    fun reset() {
        _uiState.value = BusinessUiState.Idle
    }
}



















//package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
//import okhttp3.MediaType.Companion.toMediaType
//
//import android.app.Application
//import android.content.Context
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import okhttp3.*
//import org.json.JSONObject
//import java.io.IOException
//
///** -------- UI STATE SEALED CLASS -------- */
//sealed class BusinessSetupUiState {
//    object Idle : BusinessSetupUiState()
//    object Loading : BusinessSetupUiState()
//    data class Success(val msg: String) : BusinessSetupUiState()
//    data class Error(val msg: String) : BusinessSetupUiState()
//}
//
///** -------- VIEWMODEL -------- */
//class BusinessSetupViewModel(app: Application) : AndroidViewModel(app) {
//
//    private val _uiState = MutableStateFlow<BusinessSetupUiState>(BusinessSetupUiState.Idle)
//    val uiState: StateFlow<BusinessSetupUiState> = _uiState
//
//    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//    private val client = OkHttpClient()
//
//    fun submitBusiness(businessName: String) {
//        val userId = prefs.getInt("user_id", -1)
//        if (userId == -1) {
//            _uiState.value = BusinessSetupUiState.Error("User ID not found")
//            return
//        }
//
//        // send network call off‑main
//        viewModelScope.launch {
//            _uiState.value = BusinessSetupUiState.Loading
//
//            val json = JSONObject().apply {
//                put("user_id", userId)
//                put("business_name", businessName)
//            }
//
//            val req = Request.Builder()
//                .url("https://promoted-quetzal-visually.ngrok-free.app/business/setup")
//                .post(RequestBody.create("application/json".toMediaType(), json.toString()))
//
//                .build()
//
//            client.newCall(req).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    _uiState.update { BusinessSetupUiState.Error("Network error: ${e.localizedMessage}") }
//                }
//
//                override fun onResponse(call: Call, res: Response) {
//                    if (res.isSuccessful) {
//                        _uiState.update { BusinessSetupUiState.Success("Business created!") }
//                    } else {
//                        _uiState.update { BusinessSetupUiState.Error("Server error: ${res.message}") }
//                    }
//                }
//            })
//        }
//    }
//
//    fun reset() { _uiState.value = BusinessSetupUiState.Idle }
//}

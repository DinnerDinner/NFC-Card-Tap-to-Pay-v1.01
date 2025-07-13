package com.example.nfccardtaptopayv101.ui.viewmodel.mpos

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val BASE_URL = "https://promoted-quetzal-visually.ngrok-free.app"

data class Product(
    val id: Int,
    val title: String,
    val price: Double
)

sealed class ProductMgrState {
    object Loading : ProductMgrState()
    data class Ready(val products: List<Product>) : ProductMgrState()
    data class Error(val msg: String) : ProductMgrState()
}

class ProductManagerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    private val _state = MutableStateFlow<ProductMgrState>(ProductMgrState.Loading)
    val state: StateFlow<ProductMgrState> = _state

    fun loadProducts() {
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            _state.value = ProductMgrState.Error("user_id missing")
            return
        }

        _state.value = ProductMgrState.Loading

        viewModelScope.launch {
            val body = JSONObject().put("user_id", userId)
                .toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$BASE_URL/products/list")        // <‑‑ define later
                .post(body)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _state.value = ProductMgrState.Error("Network error")
                }
                override fun onResponse(call: Call, res: Response) {
                    val arr = try { JSONObject("{\"rows\":${res.body!!.string()}}").getJSONArray("rows") }
                    catch (e: Exception) { null }
                    if (arr == null) {
                        _state.value = ProductMgrState.Error("Bad response")
                        return
                    }
                    val list = mutableListOf<Product>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list += Product(
                            id    = o.getInt("id"),
                            title = o.getString("title"),
                            price = o.optDouble("price", 0.0)
                        )
                    }
                    _state.value = ProductMgrState.Ready(list)
                }
            })
        }
    }

    /* later: add addProduct(), deleteProduct(id), etc. */
}

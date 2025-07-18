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
    val price: Double,
    val sku: String?,
    val keywords: String,
    val image_url: String?  // Add this!

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

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val reversedProducts: StateFlow<List<Product>> = state
        .map { state ->
            when (state) {
                is ProductMgrState.Ready -> state.products.reversed()
                else -> emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allFilters: StateFlow<List<String>> = state
        .map { state ->
            if (state is ProductMgrState.Ready) {
                state.products.flatMap { it.keywords.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            } else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val visibleProducts: StateFlow<List<Product>> = combine(reversedProducts, selectedFilter, searchQuery) { products, filter, query ->
        val filtered = filter?.let {
            products.filter { product ->
                product.keywords.split(",").map { it.trim() }.contains(filter)
            }
        } ?: products

        if (query.isNotBlank()) {
            filtered.filter {
                it.title.lowercase().contains(query.trim().lowercase())
            }
        } else filtered
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectFilter(filter: String?) {
        _selectedFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshProducts() {
        loadProducts()
    }

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
                .url("$BASE_URL/products/list")
                .post(body)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _state.value = ProductMgrState.Error("Network error")
                }

                override fun onResponse(call: Call, res: Response) {
                    val arr = try {
                        JSONObject("{\"rows\":${res.body!!.string()}}" ).getJSONArray("rows")
                    } catch (e: Exception) { null }

                    if (arr == null) {
                        _state.value = ProductMgrState.Error("Bad response")
                        return
                    }

                    val list = mutableListOf<Product>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list += Product(
                            id       = o.getInt("id"),
                            title    = o.getString("title"),
                            price    = o.optDouble("price", 0.0),
                            sku      = o.optString("sku", null),
                            keywords = o.optString("keywords", ""),
                            image_url = o.optString("image_url", null)  // <- Add here
                        )
                    }

                    _state.value = ProductMgrState.Ready(list)
                }
            })
        }
    }
}







//package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
//
//import android.app.Application
//import android.content.Context
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import okhttp3.*
//import org.json.JSONObject
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.toRequestBody
//import java.io.IOException
//
//private const val BASE_URL = "https://promoted-quetzal-visually.ngrok-free.app"
//
//data class Product(
//    val id: Int,
//    val title: String,
//    val price: Double,
//    val sku: String?,
//    val keywords: String // NEW
//)
//
//sealed class ProductMgrState {
//    object Loading : ProductMgrState()
//    data class Ready(val products: List<Product>) : ProductMgrState()
//    data class Error(val msg: String) : ProductMgrState()
//}
//
//class ProductManagerViewModel(app: Application) : AndroidViewModel(app) {
//
//    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//    private val client = OkHttpClient()
//
//    private val _state = MutableStateFlow<ProductMgrState>(ProductMgrState.Loading)
//    val state: StateFlow<ProductMgrState> = _state
//
//    private val _selectedFilter = MutableStateFlow<String?>(null)
//    val selectedFilter: StateFlow<String?> = _selectedFilter
//
//    // All products in reverse order
//    val reversedProducts: StateFlow<List<Product>> = state
//        .map { state ->
//            when (state) {
//                is ProductMgrState.Ready -> state.products.reversed()
//                else -> emptyList()
//            }
//        }
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    // Extract all unique keywords (flattened, trimmed, non-empty, deduplicated)
//    val allFilters: StateFlow<List<String>> = state
//        .map { state ->
//            if (state is ProductMgrState.Ready) {
//                state.products.flatMap { it.keywords.split(",") }
//                    .map { it.trim() }
//                    .filter { it.isNotEmpty() }
//                    .distinct()
//            } else emptyList()
//        }
//        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    // Filtered visible products based on selected keyword (if any)
//    val visibleProducts: StateFlow<List<Product>> = combine(reversedProducts, selectedFilter) { products, filter ->
//        filter?.let {
//            products.filter { product ->
//                product.keywords.split(",").map { it.trim() }.contains(filter)
//            }
//        } ?: products
//    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    fun selectFilter(filter: String?) {
//        _selectedFilter.value = filter
//    }
//
//    fun refreshProducts() {
//        loadProducts()
//    }
//
//    fun loadProducts() {
//        val userId = prefs.getInt("user_id", -1)
//        if (userId == -1) {
//            _state.value = ProductMgrState.Error("user_id missing")
//            return
//        }
//
//        _state.value = ProductMgrState.Loading
//
//        viewModelScope.launch {
//            val body = JSONObject().put("user_id", userId)
//                .toString().toRequestBody("application/json".toMediaType())
//
//            val req = Request.Builder()
//                .url("$BASE_URL/products/list")
//                .post(body)
//                .build()
//
//            client.newCall(req).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    _state.value = ProductMgrState.Error("Network error")
//                }
//
//                override fun onResponse(call: Call, res: Response) {
//                    val arr = try {
//                        JSONObject("{\"rows\":${res.body!!.string()}}").getJSONArray("rows")
//                    } catch (e: Exception) { null }
//
//                    if (arr == null) {
//                        _state.value = ProductMgrState.Error("Bad response")
//                        return
//                    }
//
//                    val list = mutableListOf<Product>()
//                    for (i in 0 until arr.length()) {
//                        val o = arr.getJSONObject(i)
//                        list += Product(
//                            id       = o.getInt("id"),
//                            title    = o.getString("title"),
//                            price    = o.optDouble("price", 0.0),
//                            sku      = o.optString("sku", null),
//                            keywords = o.optString("keywords", "") // Must match backend response
//                        )
//                    }
//
//                    _state.value = ProductMgrState.Ready(list)
//                }
//            })
//        }
//    }
//}
//
//
//

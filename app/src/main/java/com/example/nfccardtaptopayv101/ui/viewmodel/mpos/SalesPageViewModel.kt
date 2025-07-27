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
import com.example.nfccardtaptopayv101.repository.ProductQuantityRepository

private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"

sealed class SalesPageState {
    object Loading : SalesPageState()
    data class Error(val msg: String) : SalesPageState()
    data class Ready(
        val products: List<Product>,
        val quantities: Map<Int, Int>,
        val totalPrice: Double,
        val totalItems: Int
    ) : SalesPageState()
}

//data class Product(
//    val id: Int,
//    val title: String,
//    val price: Double,
//    val sku: String?,
//    val keywords: String,
//    val image_url: String?
//)

class SalesPageViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    // Add the repository instance
    private val quantityRepository = ProductQuantityRepository.getInstance()

    private val _state = MutableStateFlow<SalesPageState>(SalesPageState.Loading)
    val state: StateFlow<SalesPageState> = _state.asStateFlow()

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allFilters: StateFlow<List<String>> = state.map { currentState ->
        if (currentState is SalesPageState.Ready) {
            currentState.products.flatMap { it.keywords.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val visibleProducts: StateFlow<List<Product>> = combine(
        state,
        selectedFilter,
        searchQuery
    ) { currentState, filter, query ->
        if (currentState !is SalesPageState.Ready) return@combine emptyList()

        val products = currentState.products
        val filtered = filter?.let {
            products.filter { product ->
                product.keywords.split(",").map { it.trim() }.contains(filter)
            }
        } ?: products

        if (query.isNotBlank()) {
            filtered.filter {
                it.title.contains(query.trim(), ignoreCase = true)
            }
        } else filtered
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectFilter(filter: String?) {
        _selectedFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        loadProducts()
        // Observe quantity changes from the repository
        observeQuantityChanges()
    }

    private fun observeQuantityChanges() {
        viewModelScope.launch {
            quantityRepository.quantities.collect { repositoryQuantities ->
                val currentState = _state.value
                if (currentState is SalesPageState.Ready) {
                    // Use repository quantities as the source of truth
                    val updatedQuantities = repositoryQuantities.toMutableMap()

                    // Ensure all products in our list have an entry (defaulting to 0 if not in repo)
                    currentState.products.forEach { product ->
                        if (!updatedQuantities.containsKey(product.id)) {
                            updatedQuantities[product.id] = 0
                        }
                    }

                    val updatedTotalPrice = currentState.products.sumOf {
                        it.price * (updatedQuantities[it.id] ?: 0)
                    }
                    val updatedTotalItems = updatedQuantities.values.sum()

                    _state.value = currentState.copy(
                        quantities = updatedQuantities,
                        totalPrice = updatedTotalPrice,
                        totalItems = updatedTotalItems
                    )
                }
            }
        }
    }

    fun loadProducts() {
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            _state.value = SalesPageState.Error("User ID missing")
            return
        }

        _state.value = SalesPageState.Loading

        viewModelScope.launch {
            val body = JSONObject().put("user_id", userId)
                .toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$BASE_URL/products/list")
                .post(body)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    _state.value = SalesPageState.Error("Network error")
                }

                override fun onResponse(call: Call, res: Response) {
                    val arr = try {
                        JSONObject("{\"rows\":${res.body!!.string()}}")
                            .getJSONArray("rows")
                    } catch (e: Exception) {
                        null
                    }

                    if (arr == null) {
                        _state.value = SalesPageState.Error("Bad response")
                        return
                    }

                    val list = mutableListOf<Product>()
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        list += Product(
                            id = o.getInt("id"),
                            title = o.getString("title"),
                            price = o.optDouble("price", 0.0),
                            sku = o.optString("sku", null),
                            keywords = o.getString("keywords"),
                            image_url = o.optString("image_url", null)
                        )
                    }

                    val initialQuantities = list.associate { it.id to 0 }

                    // Set initial quantities in repository (only if empty)
                    quantityRepository.setInitialQuantities(initialQuantities)

                    _state.value = SalesPageState.Ready(
                        products = list,
                        quantities = initialQuantities,
                        totalPrice = 0.0,
                        totalItems = 0
                    )
                }
            })
        }
    }

    fun updateQuantity(productId: Int, newQuantity: Int) {
        val currentState = _state.value
        if (currentState !is SalesPageState.Ready) return
        if (newQuantity < 0) return

        val updatedQuantities = currentState.quantities.toMutableMap()
        updatedQuantities[productId] = newQuantity

        val updatedTotalPrice = currentState.products.sumOf {
            it.price * (updatedQuantities[it.id] ?: 0)
        }
        val updatedTotalItems = updatedQuantities.values.sum()

        _state.value = currentState.copy(
            quantities = updatedQuantities,
            totalPrice = updatedTotalPrice,
            totalItems = updatedTotalItems
        )
    }

    fun incrementQuantity(productId: Int) {
        // Update repository directly - this will trigger the observer to update UI
        quantityRepository.updateProductQuantity(productId)
    }

    fun decrementQuantity(productId: Int) {
        // Update repository directly - this will trigger the observer to update UI
        quantityRepository.decrementProductQuantity(productId)
    }

    fun getSelectedProducts(): List<Pair<Product, Int>> {
        val currentState = _state.value
        if (currentState !is SalesPageState.Ready) return emptyList()

        return currentState.products.mapNotNull { product ->
            val qty = currentState.quantities[product.id] ?: 0
            if (qty > 0) Pair(product, qty) else null
        }
    }

    fun getGrandTotal(): Double {
        val currentState = _state.value
        if (currentState !is SalesPageState.Ready) return 0.0
        return currentState.products.sumOf {
            it.price * (currentState.quantities[it.id] ?: 0)
        }
    }

    fun getTotalItemCount(): Int {
        val currentState = _state.value
        if (currentState !is SalesPageState.Ready) return 0
        return currentState.quantities.values.sum()
    }

    fun clearCart() {
        // Clear quantities in repository - this will trigger the observer to update the state
        quantityRepository.clearQuantities()
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
//import com.example.nfccardtaptopayv101.repository.ProductQuantityRepository
//
//private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"
//
//sealed class SalesPageState {
//    object Loading : SalesPageState()
//    data class Error(val msg: String) : SalesPageState()
//    data class Ready(
//        val products: List<Product>,
//        val quantities: Map<Int, Int>,
//        val totalPrice: Double,
//        val totalItems: Int
//    ) : SalesPageState()
//}
//
//class SalesPageViewModel(app: Application) : AndroidViewModel(app) {
//
//    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//    private val client = OkHttpClient()
//
//    // Add the repository instance
//    private val quantityRepository = ProductQuantityRepository.getInstance()
//
//    private val _state = MutableStateFlow<SalesPageState>(SalesPageState.Loading)
//    val state: StateFlow<SalesPageState> = _state.asStateFlow()
//
//    private val _selectedFilter = MutableStateFlow<String?>(null)
//    val selectedFilter: StateFlow<String?> = _selectedFilter
//
//    private val _searchQuery = MutableStateFlow("")
//    val searchQuery: StateFlow<String> = _searchQuery
//
//    val allFilters: StateFlow<List<String>> = state.map { currentState ->
//        if (currentState is SalesPageState.Ready) {
//            currentState.products.flatMap { it.keywords.split(",") }
//                .map { it.trim() }
//                .filter { it.isNotEmpty() }
//                .distinct()
//        } else emptyList()
//    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    val visibleProducts: StateFlow<List<Product>> = combine(
//        state,
//        selectedFilter,
//        searchQuery
//    ) { currentState, filter, query ->
//        if (currentState !is SalesPageState.Ready) return@combine emptyList()
//
//        val products = currentState.products
//        val filtered = filter?.let {
//            products.filter { product ->
//                product.keywords.split(",").map { it.trim() }.contains(filter)
//            }
//        } ?: products
//
//        if (query.isNotBlank()) {
//            filtered.filter {
//                it.title.contains(query.trim(), ignoreCase = true)
//            }
//        } else filtered
//    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//
//    fun selectFilter(filter: String?) {
//        _selectedFilter.value = filter
//    }
//
//    fun updateSearchQuery(query: String) {
//        _searchQuery.value = query
//    }
//
//    init {
//        loadProducts()
//        // Observe quantity changes from the repository
//        observeQuantityChanges()
//    }
//
//    private fun observeQuantityChanges() {
//        viewModelScope.launch {
//            quantityRepository.quantities.collect { repositoryQuantities ->
//                val currentState = _state.value
//                if (currentState is SalesPageState.Ready) {
//                    // Merge repository quantities with existing quantities
//                    val mergedQuantities = currentState.quantities.toMutableMap()
//                    repositoryQuantities.forEach { (productId, quantity) ->
//                        mergedQuantities[productId] = quantity
//                    }
//
//                    val updatedTotalPrice = currentState.products.sumOf {
//                        it.price * (mergedQuantities[it.id] ?: 0)
//                    }
//                    val updatedTotalItems = mergedQuantities.values.sum()
//
//                    _state.value = currentState.copy(
//                        quantities = mergedQuantities,
//                        totalPrice = updatedTotalPrice,
//                        totalItems = updatedTotalItems
//                    )
//                }
//            }
//        }
//    }
//
//    fun loadProducts() {
//        val userId = prefs.getInt("user_id", -1)
//        if (userId == -1) {
//            _state.value = SalesPageState.Error("User ID missing")
//            return
//        }
//
//        _state.value = SalesPageState.Loading
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
//                    _state.value = SalesPageState.Error("Network error")
//                }
//
//                override fun onResponse(call: Call, res: Response) {
//                    val arr = try {
//                        JSONObject("{\"rows\":${res.body!!.string()}}")
//                            .getJSONArray("rows")
//                    } catch (e: Exception) {
//                        null
//                    }
//
//                    if (arr == null) {
//                        _state.value = SalesPageState.Error("Bad response")
//                        return
//                    }
//
//                    val list = mutableListOf<Product>()
//                    for (i in 0 until arr.length()) {
//                        val o = arr.getJSONObject(i)
//                        list += Product(
//                            id = o.getInt("id"),
//                            title = o.getString("title"),
//                            price = o.optDouble("price", 0.0),
//                            sku = o.optString("sku", null),
//                            keywords = o.getString("keywords"),
//                            image_url = o.optString("image_url", null)
//                        )
//                    }
//
//                    val initialQuantities = list.associate { it.id to 0 }
//
//                    // Set initial quantities in repository (only if empty)
//                    quantityRepository.setInitialQuantities(initialQuantities)
//
//                    _state.value = SalesPageState.Ready(
//                        products = list,
//                        quantities = initialQuantities,
//                        totalPrice = 0.0,
//                        totalItems = 0
//                    )
//                }
//            })
//        }
//    }
//
//    fun updateQuantity(productId: Int, newQuantity: Int) {
//        val currentState = _state.value
//        if (currentState !is SalesPageState.Ready) return
//        if (newQuantity < 0) return
//
//        val updatedQuantities = currentState.quantities.toMutableMap()
//        updatedQuantities[productId] = newQuantity
//
//        val updatedTotalPrice = currentState.products.sumOf {
//            it.price * (updatedQuantities[it.id] ?: 0)
//        }
//        val updatedTotalItems = updatedQuantities.values.sum()
//
//        _state.value = currentState.copy(
//            quantities = updatedQuantities,
//            totalPrice = updatedTotalPrice,
//            totalItems = updatedTotalItems
//        )
//    }
//
//    fun incrementQuantity(productId: Int) {
//        // Update repository directly instead of going through updateQuantity
//        quantityRepository.updateProductQuantity(productId)
//    }
//
//    fun decrementQuantity(productId: Int) {
//        val currentQty = quantityRepository.getQuantity(productId)
//        if (currentQty > 0) {
//            // Manually set the new quantity in repository
//            val currentState = _state.value
//            if (currentState is SalesPageState.Ready) {
//                val updatedQuantities = currentState.quantities.toMutableMap()
//                updatedQuantities[productId] = currentQty - 1
//
//                // Update local state
//                val updatedTotalPrice = currentState.products.sumOf {
//                    it.price * (updatedQuantities[it.id] ?: 0)
//                }
//                val updatedTotalItems = updatedQuantities.values.sum()
//
//                _state.value = currentState.copy(
//                    quantities = updatedQuantities,
//                    totalPrice = updatedTotalPrice,
//                    totalItems = updatedTotalItems
//                )
//            }
//        }
//    }
//
//    fun getSelectedProducts(): List<Pair<Product, Int>> {
//        val currentState = _state.value
//        if (currentState !is SalesPageState.Ready) return emptyList()
//
//        return currentState.products.mapNotNull { product ->
//            val qty = currentState.quantities[product.id] ?: 0
//            if (qty > 0) Pair(product, qty) else null
//        }
//    }
//
//    fun getGrandTotal(): Double {
//        val currentState = _state.value
//        if (currentState !is SalesPageState.Ready) return 0.0
//        return currentState.products.sumOf {
//            it.price * (currentState.quantities[it.id] ?: 0)
//        }
//    }
//
//    fun getTotalItemCount(): Int {
//        val currentState = _state.value
//        if (currentState !is SalesPageState.Ready) return 0
//        return currentState.quantities.values.sum()
//    }
//
//    fun clearCart() {
//        val currentState = _state.value
//        if (currentState !is SalesPageState.Ready) return
//
//        val resetQuantities = currentState.products.associate { it.id to 0 }
//
//        // Clear quantities in repository as well
//        quantityRepository.clearQuantities()
//
//        _state.value = currentState.copy(
//            quantities = resetQuantities,
//            totalItems = 0,
//            totalPrice = 0.0
//        )
//    }
//}
//
//
//
////package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
////
////import android.app.Application
////import android.content.Context
////import androidx.lifecycle.AndroidViewModel
////import androidx.lifecycle.viewModelScope
////import kotlinx.coroutines.flow.*
////import kotlinx.coroutines.launch
////import okhttp3.*
////import org.json.JSONObject
////import okhttp3.MediaType.Companion.toMediaType
////import okhttp3.RequestBody.Companion.toRequestBody
////import java.io.IOException
////import com.example.nfccardtaptopayv101.repository.ProductQuantityRepository
////
////private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"
////
////sealed class SalesPageState {
////    object Loading : SalesPageState()
////    data class Error(val msg: String) : SalesPageState()
////    data class Ready(
////        val products: List<Product>,
////        val quantities: Map<Int, Int>,
////        val totalPrice: Double,
////        val totalItems: Int
////    ) : SalesPageState()
////}
////
////class SalesPageViewModel(app: Application) : AndroidViewModel(app) {
////
////    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
////    private val client = OkHttpClient()
////
////    // Add the repository instance
////    private val quantityRepository = ProductQuantityRepository.getInstance()
////
////    private val _state = MutableStateFlow<SalesPageState>(SalesPageState.Loading)
////    val state: StateFlow<SalesPageState> = _state.asStateFlow()
////
////    private val _selectedFilter = MutableStateFlow<String?>(null)
////    val selectedFilter: StateFlow<String?> = _selectedFilter
////
////    private val _searchQuery = MutableStateFlow("")
////    val searchQuery: StateFlow<String> = _searchQuery
////
////    val allFilters: StateFlow<List<String>> = state.map { currentState ->
////        if (currentState is SalesPageState.Ready) {
////            currentState.products.flatMap { it.keywords.split(",") }
////                .map { it.trim() }
////                .filter { it.isNotEmpty() }
////                .distinct()
////        } else emptyList()
////    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
////
////    val visibleProducts: StateFlow<List<Product>> = combine(
////        state,
////        selectedFilter,
////        searchQuery
////    ) { currentState, filter, query ->
////        if (currentState !is SalesPageState.Ready) return@combine emptyList()
////
////        val products = currentState.products
////        val filtered = filter?.let {
////            products.filter { product ->
////                product.keywords.split(",").map { it.trim() }.contains(filter)
////            }
////        } ?: products
////
////        if (query.isNotBlank()) {
////            filtered.filter {
////                it.title.contains(query.trim(), ignoreCase = true)
////            }
////        } else filtered
////    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
////
////    fun selectFilter(filter: String?) {
////        _selectedFilter.value = filter
////    }
////
////    fun updateSearchQuery(query: String) {
////        _searchQuery.value = query
////    }
////
////    init {
////        loadProducts()
////        // Observe quantity changes from the repository
////        observeQuantityChanges()
////    }
////
////    private fun observeQuantityChanges() {
////        viewModelScope.launch {
////            quantityRepository.quantities.collect { repositoryQuantities ->
////                val currentState = _state.value
////                if (currentState is SalesPageState.Ready) {
////                    // Merge repository quantities with existing quantities
////                    val mergedQuantities = currentState.quantities.toMutableMap()
////                    repositoryQuantities.forEach { (productId, quantity) ->
////                        mergedQuantities[productId] = quantity
////                    }
////
////                    val updatedTotalPrice = currentState.products.sumOf {
////                        it.price * (mergedQuantities[it.id] ?: 0)
////                    }
////                    val updatedTotalItems = mergedQuantities.values.sum()
////
////                    _state.value = currentState.copy(
////                        quantities = mergedQuantities,
////                        totalPrice = updatedTotalPrice,
////                        totalItems = updatedTotalItems
////                    )
////                }
////            }
////        }
////    }
////
////    fun loadProducts() {
////        val userId = prefs.getInt("user_id", -1)
////        if (userId == -1) {
////            _state.value = SalesPageState.Error("User ID missing")
////            return
////        }
////
////        _state.value = SalesPageState.Loading
////
////        viewModelScope.launch {
////            val body = JSONObject().put("user_id", userId)
////                .toString().toRequestBody("application/json".toMediaType())
////
////            val req = Request.Builder()
////                .url("$BASE_URL/products/list")
////                .post(body)
////                .build()
////
////            client.newCall(req).enqueue(object : Callback {
////                override fun onFailure(call: Call, e: IOException) {
////                    _state.value = SalesPageState.Error("Network error")
////                }
////
////                override fun onResponse(call: Call, res: Response) {
////                    val arr = try {
////                        JSONObject("{\"rows\":${res.body!!.string()}}")
////                            .getJSONArray("rows")
////                    } catch (e: Exception) {
////                        null
////                    }
////
////                    if (arr == null) {
////                        _state.value = SalesPageState.Error("Bad response")
////                        return
////                    }
////
////                    val list = mutableListOf<Product>()
////                    for (i in 0 until arr.length()) {
////                        val o = arr.getJSONObject(i)
////                        list += Product(
////                            id = o.getInt("id"),
////                            title = o.getString("title"),
////                            price = o.optDouble("price", 0.0),
////                            sku = o.optString("sku", null),
////                            keywords = o.getString("keywords"),
////                            image_url = o.optString("image_url", null)
////                        )
////                    }
////
////                    val initialQuantities = list.associate { it.id to 0 }
////
////                    // Set initial quantities in repository (only if empty)
////                    quantityRepository.setInitialQuantities(initialQuantities)
////
////                    _state.value = SalesPageState.Ready(
////                        products = list,
////                        quantities = initialQuantities,
////                        totalPrice = 0.0,
////                        totalItems = 0
////                    )
////                }
////            })
////        }
////    }
////
////    fun updateQuantity(productId: Int, newQuantity: Int) {
////        val currentState = _state.value
////        if (currentState !is SalesPageState.Ready) return
////        if (newQuantity < 0) return
////
////        val updatedQuantities = currentState.quantities.toMutableMap()
////        updatedQuantities[productId] = newQuantity
////
////        val updatedTotalPrice = currentState.products.sumOf {
////            it.price * (updatedQuantities[it.id] ?: 0)
////        }
////        val updatedTotalItems = updatedQuantities.values.sum()
////
////        _state.value = currentState.copy(
////            quantities = updatedQuantities,
////            totalPrice = updatedTotalPrice,
////            totalItems = updatedTotalItems
////        )
////    }
////
////    fun incrementQuantity(productId: Int) {
////        val qty = (_state.value as? SalesPageState.Ready)?.quantities?.get(productId) ?: 0
////        updateQuantity(productId, qty + 1)
////    }
////
////    fun decrementQuantity(productId: Int) {
////        val qty = (_state.value as? SalesPageState.Ready)?.quantities?.get(productId) ?: 0
////        if (qty > 0) updateQuantity(productId, qty - 1)
////    }
////
////    fun getSelectedProducts(): List<Pair<Product, Int>> {
////        val currentState = _state.value
////        if (currentState !is SalesPageState.Ready) return emptyList()
////
////        return currentState.products.mapNotNull { product ->
////            val qty = currentState.quantities[product.id] ?: 0
////            if (qty > 0) Pair(product, qty) else null
////        }
////    }
////
////    fun getGrandTotal(): Double {
////        val currentState = _state.value
////        if (currentState !is SalesPageState.Ready) return 0.0
////        return currentState.products.sumOf {
////            it.price * (currentState.quantities[it.id] ?: 0)
////        }
////    }
////
////    fun getTotalItemCount(): Int {
////        val currentState = _state.value
////        if (currentState !is SalesPageState.Ready) return 0
////        return currentState.quantities.values.sum()
////    }
////
////    fun clearCart() {
////        val currentState = _state.value
////        if (currentState !is SalesPageState.Ready) return
////
////        val resetQuantities = currentState.products.associate { it.id to 0 }
////
////        // Clear quantities in repository as well
////        quantityRepository.clearQuantities()
////
////        _state.value = currentState.copy(
////            quantities = resetQuantities,
////            totalItems = 0,
////            totalPrice = 0.0
////        )
////    }
////}
////
////
////
////
////
////
//////package com.example.nfccardtaptopayv101.ui.viewmodel.mpos
//////
//////import android.app.Application
//////import android.content.Context
//////import androidx.lifecycle.AndroidViewModel
//////import androidx.lifecycle.viewModelScope
//////import kotlinx.coroutines.flow.*
//////import kotlinx.coroutines.launch
//////import okhttp3.*
//////import org.json.JSONObject
//////import okhttp3.MediaType.Companion.toMediaType
//////import okhttp3.RequestBody.Companion.toRequestBody
//////import java.io.IOException
//////
//////private const val BASE_URL = "https://nfc-fastapi-backend.onrender.com"
//////
//////sealed class SalesPageState {
//////    object Loading : SalesPageState()
//////    data class Error(val msg: String) : SalesPageState()
//////    data class Ready(
//////        val products: List<Product>,
//////        val quantities: Map<Int, Int>,
//////        val totalPrice: Double,
//////        val totalItems: Int
//////    ) : SalesPageState()
//////}
//////
//////class SalesPageViewModel(app: Application) : AndroidViewModel(app) {
//////
//////    private val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//////    private val client = OkHttpClient()
//////
//////    private val _state = MutableStateFlow<SalesPageState>(SalesPageState.Loading)
//////    val state: StateFlow<SalesPageState> = _state.asStateFlow()
//////
//////    private val _selectedFilter = MutableStateFlow<String?>(null)
//////    val selectedFilter: StateFlow<String?> = _selectedFilter
//////
//////    private val _searchQuery = MutableStateFlow("")
//////    val searchQuery: StateFlow<String> = _searchQuery
//////
//////    val allFilters: StateFlow<List<String>> = state.map { currentState ->
//////        if (currentState is SalesPageState.Ready) {
//////            currentState.products.flatMap { it.keywords.split(",") }
//////                .map { it.trim() }
//////                .filter { it.isNotEmpty() }
//////                .distinct()
//////        } else emptyList()
//////    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//////
//////    val visibleProducts: StateFlow<List<Product>> = combine(
//////        state,
//////        selectedFilter,
//////        searchQuery
//////    ) { currentState, filter, query ->
//////        if (currentState !is SalesPageState.Ready) return@combine emptyList()
//////
//////        val products = currentState.products
//////        val filtered = filter?.let {
//////            products.filter { product ->
//////                product.keywords.split(",").map { it.trim() }.contains(filter)
//////            }
//////        } ?: products
//////
//////        if (query.isNotBlank()) {
//////            filtered.filter {
//////                it.title.contains(query.trim(), ignoreCase = true)
//////            }
//////        } else filtered
//////    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
//////
//////    fun selectFilter(filter: String?) {
//////        _selectedFilter.value = filter
//////    }
//////
//////    fun updateSearchQuery(query: String) {
//////        _searchQuery.value = query
//////    }
//////
//////    init {
//////        loadProducts()
//////    }
//////
//////    fun loadProducts() {
//////        val userId = prefs.getInt("user_id", -1)
//////        if (userId == -1) {
//////            _state.value = SalesPageState.Error("User ID missing")
//////            return
//////        }
//////
//////        _state.value = SalesPageState.Loading
//////
//////        viewModelScope.launch {
//////            val body = JSONObject().put("user_id", userId)
//////                .toString().toRequestBody("application/json".toMediaType())
//////
//////            val req = Request.Builder()
//////                .url("$BASE_URL/products/list")
//////                .post(body)
//////                .build()
//////
//////            client.newCall(req).enqueue(object : Callback {
//////                override fun onFailure(call: Call, e: IOException) {
//////                    _state.value = SalesPageState.Error("Network error")
//////                }
//////
//////                override fun onResponse(call: Call, res: Response) {
//////                    val arr = try {
//////                        JSONObject("{\"rows\":${res.body!!.string()}}")
//////                            .getJSONArray("rows")
//////                    } catch (e: Exception) {
//////                        null
//////                    }
//////
//////                    if (arr == null) {
//////                        _state.value = SalesPageState.Error("Bad response")
//////                        return
//////                    }
//////
//////                    val list = mutableListOf<Product>()
//////                    for (i in 0 until arr.length()) {
//////                        val o = arr.getJSONObject(i)
//////                        list += Product(
//////                            id = o.getInt("id"),
//////                            title = o.getString("title"),
//////                            price = o.optDouble("price", 0.0),
//////                            sku = o.optString("sku", null),
//////                            keywords = o.getString("keywords"),
//////                            image_url = o.optString("image_url", null)  // <- Add here
//////                        )
//////                    }
//////
//////                    val quantities = list.associate { it.id to 0 }
//////
//////                    _state.value = SalesPageState.Ready(
//////                        products = list,
//////                        quantities = quantities,
//////                        totalPrice = 0.0,
//////                        totalItems = 0
//////                    )
//////                }
//////            })
//////        }
//////    }
//////
//////    fun updateQuantity(productId: Int, newQuantity: Int) {
//////        val currentState = _state.value
//////        if (currentState !is SalesPageState.Ready) return
//////        if (newQuantity < 0) return
//////
//////        val updatedQuantities = currentState.quantities.toMutableMap()
//////        updatedQuantities[productId] = newQuantity
//////
//////        val updatedTotalPrice = currentState.products.sumOf {
//////            it.price * (updatedQuantities[it.id] ?: 0)
//////        }
//////        val updatedTotalItems = updatedQuantities.values.sum()
//////
//////        _state.value = currentState.copy(
//////            quantities = updatedQuantities,
//////            totalPrice = updatedTotalPrice,
//////            totalItems = updatedTotalItems
//////        )
//////    }
//////
//////    fun incrementQuantity(productId: Int) {
//////        val qty = (_state.value as? SalesPageState.Ready)?.quantities?.get(productId) ?: 0
//////        updateQuantity(productId, qty + 1)
//////    }
//////
//////    fun decrementQuantity(productId: Int) {
//////        val qty = (_state.value as? SalesPageState.Ready)?.quantities?.get(productId) ?: 0
//////        if (qty > 0) updateQuantity(productId, qty - 1)
//////    }
//////
//////    fun getSelectedProducts(): List<Pair<Product, Int>> {
//////        val currentState = _state.value
//////        if (currentState !is SalesPageState.Ready) return emptyList()
//////
//////        return currentState.products.mapNotNull { product ->
//////            val qty = currentState.quantities[product.id] ?: 0
//////            if (qty > 0) Pair(product, qty) else null
//////        }
//////    }
//////
//////    fun getGrandTotal(): Double {
//////        val currentState = _state.value
//////        if (currentState !is SalesPageState.Ready) return 0.0
//////        return currentState.products.sumOf {
//////            it.price * (currentState.quantities[it.id] ?: 0)
//////        }
//////    }
//////
//////    fun getTotalItemCount(): Int {
//////        val currentState = _state.value
//////        if (currentState !is SalesPageState.Ready) return 0
//////        return currentState.quantities.values.sum()
//////    }
//////
//////    fun clearCart() {
//////        val currentState = _state.value
//////        if (currentState !is SalesPageState.Ready) return
//////
//////        val resetQuantities = currentState.products.associate { it.id to 0 }
//////
//////        _state.value = currentState.copy(
//////            quantities = resetQuantities,
//////            totalItems = 0,
//////            totalPrice = 0.0
//////        )
//////    }
//////}
//////

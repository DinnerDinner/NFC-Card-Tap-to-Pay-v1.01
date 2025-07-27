package com.example.nfccardtaptopayv101.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProductQuantityRepository private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: ProductQuantityRepository? = null

        fun getInstance(): ProductQuantityRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProductQuantityRepository().also { INSTANCE = it }
            }
        }
    }

    private val _quantities = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quantities: StateFlow<Map<Int, Int>> = _quantities.asStateFlow()

    fun updateProductQuantity(productId: Int) {
        val currentQuantities = _quantities.value.toMutableMap()

        // Check if product exists, if yes +1, if not =1
        currentQuantities[productId] = if (currentQuantities.containsKey(productId)) {
            currentQuantities[productId]!! + 1
        } else {
            1
        }

        _quantities.value = currentQuantities
    }

    fun decrementProductQuantity(productId: Int) {
        val currentQuantities = _quantities.value.toMutableMap()
        val currentQty = currentQuantities[productId] ?: 0

        if (currentQty > 0) {
            currentQuantities[productId] = currentQty - 1
            _quantities.value = currentQuantities
        }
    }

    fun setProductQuantity(productId: Int, quantity: Int) {
        if (quantity >= 0) {
            val currentQuantities = _quantities.value.toMutableMap()
            currentQuantities[productId] = quantity
            _quantities.value = currentQuantities
        }
    }

    fun setInitialQuantities(initialQuantities: Map<Int, Int>) {
        // Only set if current quantities are empty to avoid overriding existing data
        if (_quantities.value.isEmpty()) {
            _quantities.value = initialQuantities
        }
    }

    fun getQuantity(productId: Int): Int {
        return _quantities.value[productId] ?: 0
    }

    fun clearQuantities() {
        _quantities.value = emptyMap()
    }
}
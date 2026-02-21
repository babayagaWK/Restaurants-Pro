package com.example.restaurantpos.ui.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class KitchenViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<KitchenUiState>(KitchenUiState.Loading)
    val uiState: StateFlow<KitchenUiState> = _uiState.asStateFlow()

    init {
        startPollingOrders()
    }

    private var knownOrderIds = setOf<Int>()
    private var isFirstLoad = true
    val newOrderAlert = MutableStateFlow<Order?>(null)

    private fun startPollingOrders() {
        viewModelScope.launch {
            repository.pollOrders(status = "pending,cooking", intervalMillis = 5000)
                .catch { e ->
                    _uiState.value = KitchenUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { orders ->
                    _uiState.value = KitchenUiState.Success(orders)
                    
                    val currentPendingIds = orders.filter { it.status == "pending" }.map { it.id }.toSet()
                    
                    if (!isFirstLoad) {
                        val newIds = currentPendingIds - knownOrderIds
                        if (newIds.isNotEmpty() && newOrderAlert.value == null) {
                            val newOrder = orders.first { it.id == newIds.first() }
                            newOrderAlert.value = newOrder
                        }
                    } else {
                        isFirstLoad = false
                    }
                    
                    knownOrderIds = knownOrderIds + currentPendingIds
                }
        }
    }

    fun dismissNewOrderAlert() {
        newOrderAlert.value = null
    }

    fun rejectOrder(orderId: Int) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "cancelled")
            dismissNewOrderAlert()
        }
    }

    // Move order from 'pending' to 'cooking', or 'cooking' to 'ready'
    fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            val result = repository.updateOrderStatus(orderId, newStatus)
            result.onSuccess {
                // Polling handles refresh
            }.onFailure { e ->
                // Handle error
            }
        }
    }
}

sealed class KitchenUiState {
    object Loading : KitchenUiState()
    data class Success(val orders: List<Order>) : KitchenUiState()
    data class Error(val message: String) : KitchenUiState()
}

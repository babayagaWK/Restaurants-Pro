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

    private fun startPollingOrders() {
        viewModelScope.launch {
            // Poll both pending and cooking orders every 5 seconds
            repository.pollOrders(status = "pending,cooking", intervalMillis = 5000)
                .catch { e ->
                    _uiState.value = KitchenUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { orders ->
                    _uiState.value = KitchenUiState.Success(orders)
                }
        }
    }

    // Move order from 'pending' to 'cooking', or 'cooking' to 'ready'
    fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            // Optionally, set state to loading/updating here if needed
            val result = repository.updateOrderStatus(orderId, newStatus)
            result.onSuccess {
                // The polling flow will automatically fetch the updated list on next tick.
                // But we could also immediately refresh if we want:
                // refreshOrders()
            }.onFailure { e ->
                // Handle error (e.g., show a Toast or snackbar via SharedFlow)
            }
        }
    }
}

sealed class KitchenUiState {
    object Loading : KitchenUiState()
    data class Success(val orders: List<Order>) : KitchenUiState()
    data class Error(val message: String) : KitchenUiState()
}

package com.example.restaurantpos.data.repository

import com.example.restaurantpos.data.api.PosApiService
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.model.OrderRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class OrderRepository(private val apiService: PosApiService) {

    // Poll for new orders
    fun pollOrders(status: String = "pending,cooking", intervalMillis: Long = 5000): Flow<List<Order>> = flow {
        while (true) {
            try {
                // Fetch orders with specific statuses
                val response = apiService.getOrders(status = status) 
                if (response.isSuccessful) {
                    response.body()?.let { orders ->
                        emit(orders)
                    }
                } else {
                    // Throw to be caught by ViewModel
                    throw Exception("Polling error: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                // Re-throw so ViewModel catch block handles it
                throw e
            }
            delay(intervalMillis)
        }
    }

    suspend fun createOrder(orderRequest: OrderRequest): Result<Order> {
        return try {
            val response = apiService.createOrder(orderRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body response"))
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: Int, newStatus: String): Result<Order> {
        return try {
            val statusUpdate = mapOf("status" to newStatus)
            val response = apiService.updateOrderStatus(orderId, statusUpdate)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty body response"))
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

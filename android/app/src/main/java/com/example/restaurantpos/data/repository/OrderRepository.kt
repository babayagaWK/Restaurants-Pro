package com.example.restaurantpos.data.repository

import com.example.restaurantpos.data.api.PosApiService
import com.example.restaurantpos.data.model.Order
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
                    // Handle error (log it, but keep polling or emit partial error)
                    println("Polling error: ${response.code()}")
                }
            } catch (e: IOException) {
                // Network error, wait and retry
                println("Polling network exception: ${e.message}")
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

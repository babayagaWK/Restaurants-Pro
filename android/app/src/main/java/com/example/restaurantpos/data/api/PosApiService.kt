package com.example.restaurantpos.data.api

import com.example.restaurantpos.data.model.Category
import com.example.restaurantpos.data.model.MenuItem
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.model.OrderRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PosApiService {

    @GET("api/categories/")
    suspend fun getCategories(): Response<List<Category>>

    @GET("api/menu-items/")
    suspend fun getMenuItems(): Response<List<MenuItem>>

    @POST("api/orders/")
    suspend fun createOrder(@Body order: OrderRequest): Response<Order>

    @GET("api/orders/")
    suspend fun getOrders(@Query("status") status: String? = null): Response<List<Order>>

    @retrofit2.http.PATCH("api/orders/{id}/")
    suspend fun updateOrderStatus(
        @retrofit2.http.Path("id") orderId: Int,
        @Body statusUpdate: Map<String, String>
    ): Response<Order>
}

package com.example.restaurantpos.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    val id: Int,
    val name: String
)

data class MenuItem(
    val id: Int,
    val category: Int,
    @SerializedName("category_name") val categoryName: String,
    val name: String,
    val description: String,
    val price: Double,
    @SerializedName("is_available") val isAvailable: Boolean,
    @SerializedName("image_url") val imageUrl: String?
)

data class OrderItemRequest(
    @SerializedName("menu_item") val menuItemId: Int,
    val quantity: Int,
    val notes: String = ""
)

data class OrderRequest(
    @SerializedName("table_number") val tableNumber: Int,
    val items: List<OrderItemRequest>
)

data class OrderItem(
    val id: Int,
    @SerializedName("menu_item") val menuItemId: Int,
    @SerializedName("menu_item_name") val menuItemName: String,
    val quantity: Int,
    val notes: String
)

data class Order(
    val id: Int,
    @SerializedName("table_number") val tableNumber: Int,
    val status: String, // "pending", "cooking", "ready", "completed", "cancelled"
    @SerializedName("created_at") val createdAt: String,
    val items: List<OrderItem>
)

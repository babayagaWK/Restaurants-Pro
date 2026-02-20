package com.example.restaurantpos.ui.kitchen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.model.OrderItem
import com.example.restaurantpos.ui.kitchen.KitchenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(viewModel: KitchenViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Display System", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121212))
        ) {
            when (val state = uiState) {
                is KitchenUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFFFB300))
                }
                is KitchenUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is KitchenUiState.Success -> {
                    KitchenBoard(
                        orders = state.orders,
                        onUpdateStatus = { orderId, newStatus ->
                            viewModel.updateOrderStatus(orderId, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KitchenBoard(orders: List<Order>, onUpdateStatus: (Int, String) -> Unit) {
    val pendingOrders = orders.filter { it.status == "pending" }
    val cookingOrders = orders.filter { it.status == "cooking" }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pending Column
        OrderColumn(
            title = "NEW ORDERS (${pendingOrders.size})",
            orders = pendingOrders,
            modifier = Modifier.weight(1f),
            actionText = "Start Cooking",
            actionColor = Color(0xFF2196F3),
            onAction = { order -> onUpdateStatus(order.id, "cooking") }
        )

        // Cooking Column
        OrderColumn(
            title = "IN PROGRESS (${cookingOrders.size})",
            orders = cookingOrders,
            modifier = Modifier.weight(1f),
            actionText = "Ready to Serve",
            actionColor = Color(0xFF4CAF50),
            onAction = { order -> onUpdateStatus(order.id, "ready") }
        )
    }
}

@Composable
fun OrderColumn(
    title: String,
    orders: List<Order>,
    modifier: Modifier = Modifier,
    actionText: String,
    actionColor: Color,
    onAction: (Order) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                OrderTicket(
                    order = order,
                    actionText = actionText,
                    actionColor = actionColor,
                    onAction = { onAction(order) }
                )
            }
        }
    }
}

@Composable
fun OrderTicket(
    order: Order,
    actionText: String,
    actionColor: Color,
    onAction: () -> Unit
) {
    val ticketColor = if (order.status == "pending") Color(0xFFFFF9C4) else Color(0xFFE3F2FD) // Light yellow for pending, light blue for cooking
    val textColor = Color.Black

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ticketColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Table ${order.tableNumber}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = "#${order.id}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Time: ${order.createdAt.substring(11, 19)}", // Assuming ISO format
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.5f))

            // Items
            order.items.forEach { item ->
                OrderItemRow(item)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrderItemRow(item: OrderItem) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${item.quantity}x ${item.menuItemName}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        if (item.notes.isNotBlank()) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Notes",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFE65100) // Deep Orange
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.notes,
                    fontSize = 14.sp,
                    color = Color(0xFFE65100),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

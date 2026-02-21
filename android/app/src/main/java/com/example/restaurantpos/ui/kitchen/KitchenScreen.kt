package com.example.restaurantpos.ui.kitchen

import android.media.RingtoneManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.model.OrderItem
import com.example.restaurantpos.ui.kitchen.KitchenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(viewModel: KitchenViewModel, onResetUrl: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Audio Notification Effect
    var previousOrderCount by remember { mutableStateOf(0) }

    LaunchedEffect(uiState) {
        if (uiState is KitchenUiState.Success) {
            val orders = (uiState as KitchenUiState.Success).orders
            val currentCount = orders.filter { it.status == "pending" }.size
            
            if (currentCount > previousOrderCount) {
                // New order arrived! Play sound
                try {
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(context, notification)
                    r.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            previousOrderCount = currentCount
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Display System", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onResetUrl) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Change Server URL",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF2C3E50)) // Dark Blue-Gray Theme
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
    // Combine and sort orders by arrival time (oldest first)
    val allOrders = orders.filter { it.status == "pending" || it.status == "cooking" }
        .sortedBy { it.createdAt }

    if (allOrders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "ไม่มีออเดอร์ค้างอยู่",
                color = Color(0xFFF1C40F),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 350.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(allOrders) { order ->
                OrderTicket(
                    order = order,
                    actionText = if (order.status == "pending") "เริ่มทำอาหาร" else "ทำเสร็จแล้ว",
                    actionColor = if (order.status == "pending") Color(0xFF3498DB) else Color(0xFFE74C3C),
                    onAction = { 
                        val nextStatus = if (order.status == "pending") "cooking" else "ready"
                        onUpdateStatus(order.id, nextStatus) 
                    }
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
    // Checkbox State Map: Maps OrderItem.id to Boolean
    val itemStates = remember(order.id) { 
        mutableStateMapOf<Int, Boolean>().apply {
            order.items.forEach { put(it.id, false) }
        }
    }
    
    // Check if ALL items in this specific order are checked
    val allChecked = if (order.items.isEmpty()) true else order.items.all { itemStates[it.id] == true }

    // Order Type Logic based on table_number
    val orderTypeColor = when {
        order.tableNumber == 0 -> Color(0xFF3498DB) // Takeaway (Blue)
        order.tableNumber < 0 -> Color(0xFFE67E22)  // Delivery (Orange)
        else -> Color(0xFF27AE60)                   // Dine-in (Green)
    }
    
    val orderTypeLabel = when {
        order.tableNumber == 0 -> "Takeaway"
        order.tableNumber < 0 -> "Delivery"
        else -> "Table ${order.tableNumber}"
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECF0F1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Thick Colored Top Border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(orderTypeColor)
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                // Header (Type & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge
                    Surface(
                        color = orderTypeColor,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = orderTypeLabel,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    
                    val timeStr = if (order.createdAt.length >= 19) {
                        order.createdAt.substring(11, 19)
                    } else {
                        order.createdAt
                    }
                    Text(
                        text = "Time: $timeStr",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))

                // Items list with checkboxes
                order.items.forEach { item ->
                    val isChecked = itemStates[item.id] ?: false
                    OrderItemRow(
                        item = item, 
                        isChecked = isChecked, 
                        onCheckedChange = { checked -> itemStates[item.id] = checked }
                    )
                }
            }

            // Action Button (Full Width Bottom)
            Button(
                onClick = onAction,
                enabled = allChecked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (order.status == "pending") Color(0xFF3498DB) else Color(0xFFE74C3C),
                    disabledContainerColor = Color(0xFFBDC3C7)
                ),
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (allChecked) Color.White else Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun OrderItemRow(
    item: OrderItem, 
    isChecked: Boolean, 
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (isChecked) Color.Gray else Color(0xFF34495E)
    val textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF27AE60),
                uncheckedColor = Color.Gray
            )
        )
        
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = "${item.quantity}x ${item.menuItemName}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textDecoration = textDecoration
            )
            
            if (item.notes.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Notes",
                        modifier = Modifier.size(14.dp),
                        tint = if (isChecked) Color.Gray else Color(0xFFE67E22)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.notes,
                        fontSize = 14.sp,
                        color = if (isChecked) Color.Gray else Color(0xFFE67E22),
                        fontWeight = FontWeight.Medium,
                        textDecoration = textDecoration
                    )
                }
            }
        }
    }
}

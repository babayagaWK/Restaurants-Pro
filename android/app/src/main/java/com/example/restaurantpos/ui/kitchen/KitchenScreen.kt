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
import android.widget.Toast
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.model.OrderItem
import com.example.restaurantpos.ui.kitchen.KitchenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(viewModel: KitchenViewModel, onResetUrl: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val newOrderAlert by viewModel.newOrderAlert.collectAsState()

    // Looping alarm effect for new orders
    LaunchedEffect(newOrderAlert) {
        if (newOrderAlert != null) {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            var ringtone: android.media.Ringtone? = null
            try {
                ringtone = RingtoneManager.getRingtone(context, notificationUri)
                while (true) {
                    if (ringtone?.isPlaying == false) {
                        ringtone.play()
                    }
                    kotlinx.coroutines.delay(2500)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ringtone?.stop()
            }
        }
    }

    var showBillDialog by remember { mutableStateOf<Order?>(null) }

    if (showBillDialog != null) {
        val order = showBillDialog!!
        val tableInfo = when {
            order.tableNumber == 0 -> "Takeaway"
            order.tableNumber < 0 -> "Delivery"
            else -> "Table ${order.tableNumber}"
        }
        AlertDialog(
            onDismissRequest = { showBillDialog = null },
            title = {
                Text("ใบเสร็จ / Receipt", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            },
            text = {
                Column {
                    Text("ออเดอร์ ID: ${order.id}", fontWeight = FontWeight.Bold)
                    Text("ประเภท: $tableInfo")
                    Text("เวลา: ${order.createdAt}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    order.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.menuItemName}", modifier = Modifier.weight(1f))
                        }
                        if (item.notes.isNotBlank()) {
                            Text("  * ${item.notes}", color = Color(0xFFE67E22), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                }
            },
            confirmButton = {
                Button(onClick = { showBillDialog = null }) {
                    Text("ปิด (Close)")
                }
            }
        )
    }

    if (newOrderAlert != null) {
        val order = newOrderAlert!!
        val tableInfo = when {
            order.tableNumber == 0 -> "Takeaway"
            order.tableNumber < 0 -> "Delivery"
            else -> "Table ${order.tableNumber}"
        }
        AlertDialog(
            onDismissRequest = { /* Explicit dismiss required */ },
            title = {
                Text(
                    text = "🔔 ออเดอร์ใหม่เข้า!",
                    color = Color(0xFFE74C3C),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = tableInfo,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    order.items.forEach { item ->
                        Text("• ${item.quantity}x ${item.menuItemName}", fontSize = 18.sp, color = Color.DarkGray)
                        if (item.notes.isNotBlank()) {
                            Text("  (${item.notes})", color = Color(0xFFE67E22), fontSize = 14.sp)
                        }
                    }
                }
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissNewOrderAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60))
                ) {
                    Text("รับทราบ (ปิดเสียง)", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.rejectOrder(order.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF95A5A6))
                ) {
                    Text("ปฏิเสธออเดอร์", color = Color.White)
                }
            }
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("กำลังทำ (Active)", "เสร็จแล้ว (Completed)")

    Scaffold(
        topBar = {
            Column {
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
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
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
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connection Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = Color(0xFFBDC3C7),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onResetUrl) {
                            Text("Change Settings")
                        }
                    }
                }
                is KitchenUiState.Success -> {
                    KitchenBoard(
                        orders = state.orders,
                        selectedTabIndex = selectedTabIndex,
                        onUpdateStatus = { orderId, newStatus ->
                            viewModel.updateOrderStatus(orderId, newStatus)
                        },
                        onReject = { orderId ->
                            viewModel.rejectOrder(orderId)
                        },
                        onViewBill = { order ->
                            showBillDialog = order
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KitchenBoard(
    orders: List<Order>, 
    selectedTabIndex: Int, 
    onUpdateStatus: (Int, String) -> Unit,
    onReject: (Int) -> Unit,
    onViewBill: (Order) -> Unit
) {
    // Combine and sort orders by arrival time (oldest first)
    val targetStatuses = if (selectedTabIndex == 0) listOf("pending", "cooking") else listOf("ready", "completed")
    val allOrders = orders.filter { it.status in targetStatuses }.sortedBy { it.createdAt }

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
                    },
                    onReject = { onReject(order.id) },
                    onViewBill = { onViewBill(order) },
                    isCompletedTab = selectedTabIndex == 1
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
    onAction: () -> Unit,
    onReject: () -> Unit,
    onViewBill: () -> Unit,
    isCompletedTab: Boolean
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

            // Action Buttons
            if (isCompletedTab) {
                Button(
                    onClick = onViewBill,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF95A5A6)),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("ดูบิล (View Bill)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Button(
                        onClick = onViewBill,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF95A5A6)),
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 0.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("ดูบิล", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    if (order.status == "pending") {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("ปฏิเสธ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onAction,
                        enabled = allChecked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (order.status == "pending") Color(0xFF3498DB) else Color(0xFF27AE60),
                            disabledContainerColor = Color(0xFFBDC3C7)
                        ),
                        shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 8.dp),
                        modifier = Modifier.weight(1.5f).fillMaxHeight()
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

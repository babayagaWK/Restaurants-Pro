package com.example.restaurantpos.ui.kitchen

import android.media.RingtoneManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import android.widget.Toast
import com.example.restaurantpos.data.model.Order
import com.example.restaurantpos.data.model.OrderItem
import com.example.restaurantpos.ui.kitchen.KitchenUiState
import com.example.restaurantpos.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    var showRejectConfirm by remember { mutableStateOf<Int?>(null) }

    // Bill Dialog
    if (showBillDialog != null) {
        BillDialog(order = showBillDialog!!, onDismiss = { showBillDialog = null })
    }

    // Reject Confirmation Dialog
    if (showRejectConfirm != null) {
        AlertDialog(
            onDismissRequest = { showRejectConfirm = null },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text("⚠️ ยืนยันปฏิเสธออเดอร์", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("คุณต้องการปฏิเสธออเดอร์นี้ใช่หรือไม่? การดำเนินการนี้ไม่สามารถย้อนกลับได้")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectOrder(showRejectConfirm!!)
                        showRejectConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text("ปฏิเสธออเดอร์", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirm = null }) {
                    Text("ยกเลิก", color = TextSecondary)
                }
            }
        )
    }

    // New Order Alert Dialog
    if (newOrderAlert != null) {
        NewOrderAlertDialog(
            order = newOrderAlert!!,
            onDismiss = { viewModel.dismissNewOrderAlert() },
            onReject = { viewModel.rejectOrder(newOrderAlert!!.id) }
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("กำลังทำ", "เสร็จแล้ว")

    // Calculate order counts for tab badges
    val orderCounts = remember(uiState) {
        when (val state = uiState) {
            is KitchenUiState.Success -> {
                val activeCount = state.orders.count { it.status in listOf("pending", "cooking") }
                val completedCount = state.orders.count { it.status in listOf("ready", "completed") }
                listOf(activeCount, completedCount)
            }
            else -> listOf(0, 0)
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Premium Top App Bar
                Surface(
                    color = DarkBase,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Gold accent dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(BrandGoldLight, BrandGoldDark)
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kitchen Display",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "FoodPOS KDS",
                                    fontSize = 12.sp,
                                    color = BrandAmberGold,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        IconButton(onClick = onResetUrl) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = tabPositions[selectedTabIndex].left)
                                    .width(tabPositions[selectedTabIndex].width)
                                    .height(3.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(BrandGoldLight, BrandAmberGold)
                                        )
                                    )
                            )
                        }
                    },
                    divider = {
                        Divider(color = BorderSubtle, thickness = 1.dp)
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTabIndex == index) BrandAmberGold else TextSecondary
                                    )
                                    if (orderCounts[index] > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = if (index == 0) AccentDanger else AccentSuccess,
                                            shape = CircleShape,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${orderCounts[index]}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                .background(DarkBase)
        ) {
            when (val state = uiState) {
                is KitchenUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = BrandAmberGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "กำลังเชื่อมต่อ...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
                is KitchenUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentDanger.copy(alpha = 0.15f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AccentDanger,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "ไม่สามารถเชื่อมต่อได้",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onResetUrl,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandAmberGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("เปลี่ยนการตั้งค่า", fontWeight = FontWeight.Bold)
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
                            showRejectConfirm = orderId
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
    val targetStatuses = if (selectedTabIndex == 0) listOf("pending", "cooking") else listOf("ready", "completed")
    val allOrders = orders.filter { it.status in targetStatuses }.sortedBy { it.createdAt }

    if (allOrders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (selectedTabIndex == 0) "🍳" else "✅",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (selectedTabIndex == 0) "ไม่มีออเดอร์ที่รอดำเนินการ" else "ไม่มีออเดอร์ที่เสร็จแล้ว",
                    color = TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (selectedTabIndex == 0) "ออเดอร์ใหม่จะปรากฏที่นี่" else "ออเดอร์ที่ทำเสร็จจะแสดงที่นี่",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(allOrders, key = { it.id }) { order ->
                OrderTicket(
                    order = order,
                    actionText = when (order.status) {
                        "pending" -> "🔥 เริ่มทำ"
                        "cooking" -> "✅ ทำเสร็จ"
                        else -> ""
                    },
                    actionColor = when (order.status) {
                        "pending" -> StatusCooking
                        "cooking" -> StatusReady
                        else -> StatusCompleted
                    },
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
    val itemStates = remember(order.id) {
        mutableStateMapOf<Int, Boolean>().apply {
            order.items.forEach { put(it.id, false) }
        }
    }

    val allChecked = if (order.items.isEmpty()) true else order.items.all { itemStates[it.id] == true }

    val orderTypeColor = when {
        order.tableNumber == 0 -> OrderTakeaway
        order.tableNumber < 0 -> OrderDelivery
        else -> OrderDineIn
    }

    val orderTypeLabel = when {
        order.tableNumber == 0 -> "🛍 Takeaway"
        order.tableNumber < 0 -> "🚚 Delivery"
        else -> "🪑 โต๊ะ ${order.tableNumber}"
    }

    val statusLabel = when (order.status) {
        "pending" -> "⏳ รอรับ"
        "cooking" -> "🔥 กำลังทำ"
        "ready" -> "✅ พร้อมเสิร์ฟ"
        "completed" -> "☑️ เสร็จสิ้น"
        "cancelled" -> "❌ ยกเลิก"
        else -> order.status
    }

    // Calculate elapsed time
    val elapsedText = remember(order.createdAt) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            // Try parsing with timezone offset
            val cleanedDate = order.createdAt.take(19)
            val date = sdf.parse(cleanedDate)
            if (date != null) {
                val diff = System.currentTimeMillis() - date.time
                val minutes = diff / 60000
                when {
                    minutes < 1 -> "เมื่อสักครู่"
                    minutes < 60 -> "${minutes} นาทีที่แล้ว"
                    else -> "${minutes / 60} ชม. ${minutes % 60} นาที"
                }
            } else order.createdAt.take(8)
        } catch (e: Exception) {
            if (order.createdAt.length >= 19) order.createdAt.substring(11, 19) else order.createdAt
        }
    }

    // Total price for this order
    val orderTotal = order.items.sumOf { it.price * it.quantity }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = BorderStroke(1.dp, BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Gradient Top Accent Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(orderTypeColor, orderTypeColor.copy(alpha = 0.3f))
                        )
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = orderTypeLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "#${order.id}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        // Status badge
                        Surface(
                            color = when (order.status) {
                                "pending" -> StatusPending.copy(alpha = 0.15f)
                                "cooking" -> StatusCooking.copy(alpha = 0.15f)
                                "ready" -> StatusReady.copy(alpha = 0.15f)
                                else -> StatusCompleted.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (order.status) {
                                    "pending" -> StatusPending
                                    "cooking" -> StatusCooking
                                    "ready" -> StatusReady
                                    else -> StatusCompleted
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = elapsedText,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Items list with checkboxes
                order.items.forEach { item ->
                    val isChecked = itemStates[item.id] ?: false
                    OrderItemRow(
                        item = item,
                        isChecked = isChecked,
                        onCheckedChange = { checked -> itemStates[item.id] = checked },
                        showPrice = true
                    )
                }

                // Order Total
                if (orderTotal > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = BorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ยอดรวม",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "฿${"%.2f".format(orderTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandAmberGold
                        )
                    }
                }
            }

            // Action Buttons
            if (isCompletedTab) {
                Button(
                    onClick = onViewBill,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHover),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 0.dp, topEnd = 0.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("📋 ดูบิล", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    // View Bill button
                    Button(
                        onClick = onViewBill,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHover),
                        shape = RoundedCornerShape(bottomStart = 16.dp, topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Text("📋 บิล", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }

                    // Reject button (only for pending)
                    if (order.status == "pending") {
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentDanger.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Text("✕ ปฏิเสธ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentDanger)
                        }
                    }

                    // Main action button
                    Button(
                        onClick = onAction,
                        enabled = allChecked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionColor,
                            disabledContainerColor = DarkSurfaceHover
                        ),
                        shape = RoundedCornerShape(bottomEnd = 16.dp, topEnd = 0.dp, topStart = 0.dp, bottomStart = 0.dp),
                        modifier = Modifier.weight(1.5f).fillMaxHeight()
                    ) {
                        Text(
                            text = actionText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allChecked) Color.White else TextMuted
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
    onCheckedChange: (Boolean) -> Unit,
    showPrice: Boolean = false
) {
    val textColor = if (isChecked) TextMuted else TextPrimary
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
                checkedColor = AccentSuccess,
                uncheckedColor = TextMuted,
                checkmarkColor = Color.White
            ),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.quantity}x ${item.menuItemName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textDecoration = textDecoration
            )

            if (item.notes.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 ",
                        fontSize = 12.sp
                    )
                    Text(
                        text = item.notes,
                        fontSize = 13.sp,
                        color = if (isChecked) TextMuted else AccentWarning,
                        fontWeight = FontWeight.Medium,
                        textDecoration = textDecoration
                    )
                }
            }
        }

        if (showPrice && item.price > 0) {
            Text(
                text = "฿${"%.0f".format(item.price * item.quantity)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChecked) TextMuted else TextSecondary,
                textDecoration = textDecoration
            )
        }
    }
}

// === Bill Dialog (Premium Full-Screen Style) ===

@Composable
fun BillDialog(order: Order, onDismiss: () -> Unit) {
    val tableInfo = when {
        order.tableNumber == 0 -> "🛍 Takeaway"
        order.tableNumber < 0 -> "🚚 Delivery"
        else -> "🪑 โต๊ะ ${order.tableNumber}"
    }
    val orderTotal = order.items.sumOf { it.price * it.quantity }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 ใบเสร็จ",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Order info
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ออเดอร์", color = TextSecondary, fontSize = 13.sp)
                            Text("#${order.id}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ประเภท", color = TextSecondary, fontSize = 13.sp)
                            Text(tableInfo, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("เวลาสั่ง", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = if (order.createdAt.length >= 19) order.createdAt.substring(11, 19) else order.createdAt,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Items
                Text(
                    text = "รายการ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.quantity}x ${item.menuItemName}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = "📝 ${item.notes}",
                                    fontSize = 12.sp,
                                    color = AccentWarning
                                )
                            }
                        }
                        if (item.price > 0) {
                            Text(
                                text = "฿${"%.2f".format(item.price * item.quantity)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Total
                if (orderTotal > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BorderHighlight, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ยอดรวมทั้งหมด",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "฿${"%.2f".format(orderTotal)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandAmberGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAmberGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ปิด", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// === New Order Alert Dialog ===

@Composable
fun NewOrderAlertDialog(order: Order, onDismiss: () -> Unit, onReject: () -> Unit) {
    val tableInfo = when {
        order.tableNumber == 0 -> "🛍 Takeaway"
        order.tableNumber < 0 -> "🚚 Delivery"
        else -> "🪑 โต๊ะ ${order.tableNumber}"
    }

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AlertDialog(
        onDismissRequest = { /* Must explicitly dismiss */ },
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔔",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "ออเดอร์ใหม่!",
                    color = AccentDanger.copy(alpha = pulseAlpha),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Surface(
                    color = DarkSurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tableInfo,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        order.items.forEach { item ->
                            Text(
                                "• ${item.quantity}x ${item.menuItemName}",
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            if (item.notes.isNotBlank()) {
                                Text(
                                    "  📝 ${item.notes}",
                                    color = AccentWarning,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentSuccess),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("✓ รับทราบ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                border = BorderStroke(1.dp, AccentDanger.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("✕ ปฏิเสธ", color = AccentDanger, fontWeight = FontWeight.Bold)
            }
        }
    )
}

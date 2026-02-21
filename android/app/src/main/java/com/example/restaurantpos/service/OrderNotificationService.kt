package com.example.restaurantpos.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.restaurantpos.MainActivity
import com.example.restaurantpos.data.api.PosApiService
import com.example.restaurantpos.data.model.Order
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OrderNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "kds_foreground_channel"
        const val ALERT_CHANNEL_ID = "kds_alert_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID_BASE = 100
        const val EXTRA_SERVER_URL = "server_url"
        private const val TAG = "OrderNotifService"
        private const val POLL_INTERVAL_MS = 15000L
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var knownPendingIds = setOf<Int>()
    private var isFirstPoll = true
    private var apiService: PosApiService? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        // Build Retrofit
        val client = OkHttpClient.Builder().build()
        val retrofit = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(PosApiService::class.java)

        // Reset state
        knownPendingIds = emptySet()
        isFirstPoll = true

        // Start foreground
        val notification = buildForegroundNotification()
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        // Start polling
        startPolling()

        return START_STICKY
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val response = apiService?.getOrders("pending")
                    val orders: List<Order> = response?.body() ?: emptyList()
                    val currentIds = orders.map { it.id }.toSet()

                    if (!isFirstPoll) {
                        val newIds = currentIds - knownPendingIds
                        if (newIds.isNotEmpty()) {
                            val newOrders = orders.filter { it.id in newIds }
                            showNewOrderNotification(newOrders)
                        }
                    } else {
                        isFirstPoll = false
                    }

                    knownPendingIds = currentIds

                    // Update foreground notification with count
                    val pendingCount = currentIds.size
                    val updatedNotification = buildForegroundNotification(pendingCount)
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(FOREGROUND_NOTIFICATION_ID, updatedNotification)

                } catch (e: Exception) {
                    Log.e(TAG, "Poll error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun showNewOrderNotification(orders: List<Order>) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        for ((index, order) in orders.withIndex()) {
            val tableInfo = when {
                order.tableNumber == 0 -> "Takeaway"
                order.tableNumber < 0 -> "Delivery"
                else -> "โต๊ะ ${order.tableNumber}"
            }

            val itemsSummary = order.items.joinToString(", ") {
                "${it.quantity}x ${it.menuItemName}"
            }

            val tapIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, order.id, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🔔 ออเดอร์ใหม่! — $tableInfo")
                .setContentText(itemsSummary)
                .setStyle(NotificationCompat.BigTextStyle().bigText(itemsSummary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()

            nm.notify(ALERT_NOTIFICATION_ID_BASE + order.id, notification)
        }
    }

    private fun buildForegroundNotification(pendingCount: Int = 0): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (pendingCount > 0) {
            "มี $pendingCount ออเดอร์ที่รอดำเนินการ"
        } else {
            "กำลังรอออเดอร์ใหม่..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("FoodPOS KDS")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground channel (silent)
            val fgChannel = NotificationChannel(
                CHANNEL_ID,
                "KDS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "แสดงสถานะการทำงานของ KDS"
            }
            nm.createNotificationChannel(fgChannel)

            // Alert channel (with sound + vibration)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "ออเดอร์ใหม่",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "แจ้งเตือนเมื่อมีออเดอร์ใหม่เข้ามา"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            nm.createNotificationChannel(alertChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

package com.example.restaurantpos

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.restaurantpos.data.api.PosApiService
import com.example.restaurantpos.data.repository.OrderRepository
import com.example.restaurantpos.ui.kitchen.KitchenScreen
import com.example.restaurantpos.ui.kitchen.KitchenViewModel
import com.example.restaurantpos.ui.theme.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val sharedPref = getSharedPreferences("KDS_PREFS", Context.MODE_PRIVATE)
        val initialUrl = sharedPref.getString("SERVER_URL", "") ?: ""

        setContent {
            FoodPOSTheme {
                var serverUrl by remember { mutableStateOf(initialUrl) }

                if (serverUrl.isEmpty()) {
                    ConfigScreen(onUrlSet = { input ->
                        var domain = input.trim()
                        if (!domain.contains(".")) {
                            domain = "$domain.pythonanywhere.com"
                        }

                        val fullUrl = if (!domain.startsWith("http")) "https://$domain" else domain

                        val uri = java.net.URI(fullUrl)
                        val base = "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}/"

                        sharedPref.edit().putString("SERVER_URL", base).apply()
                        serverUrl = base
                    })
                } else {
                    val logging = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    val client = OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .build()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(serverUrl)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    val apiService = retrofit.create(PosApiService::class.java)
                    val repository = OrderRepository(apiService)

                    val viewModel: KitchenViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return KitchenViewModel(repository) as T
                            }
                        }
                    )
                    KitchenScreen(
                        viewModel = viewModel,
                        onResetUrl = {
                            sharedPref.edit().remove("SERVER_URL").apply()
                            serverUrl = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigScreen(onUrlSet: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBase)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo / Branding
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandGoldLight, BrandGoldDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍳",
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FoodPOS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandAmberGold
            )

            Text(
                text = "Kitchen Display System",
                fontSize = 16.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Connection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "เชื่อมต่อกับเซิร์ฟเวอร์",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ระบุ PythonAnywhere Username\nของคุณเพื่อเชื่อมต่อกับระบบ",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Username", color = TextSecondary) },
                        placeholder = { Text("เช่น Siripong", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandAmberGold,
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = BrandAmberGold,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = BrandAmberGold
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onUrlSet(text.trim())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAmberGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = text.isNotBlank()
                    ) {
                        Text(
                            "เชื่อมต่อ →",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Help text
            Text(
                text = "หรือใส่ URL เต็ม เช่น https://example.com",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

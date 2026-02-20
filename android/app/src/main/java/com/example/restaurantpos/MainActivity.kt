package com.example.restaurantpos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.restaurantpos.data.api.PosApiService
import com.example.restaurantpos.data.repository.OrderRepository
import com.example.restaurantpos.ui.kitchen.KitchenScreen
import com.example.restaurantpos.ui.kitchen.KitchenViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getSharedPreferences("KDS_PREFS", Context.MODE_PRIVATE)
        val initialUrl = sharedPref.getString("SERVER_URL", "") ?: ""

        setContent {
            var serverUrl by remember { mutableStateOf(initialUrl) }

            if (serverUrl.isEmpty()) {
                ConfigScreen(onUrlSet = { url ->
                    // Make sure the URL ends with a slash
                    val formattedUrl = if (url.endsWith("/")) url else "$url/"
                    val finalUrl = if (!formattedUrl.startsWith("http")) "http://$formattedUrl" else formattedUrl
                    
                    sharedPref.edit().putString("SERVER_URL", finalUrl).apply()
                    serverUrl = finalUrl
                })
            } else {
                val retrofit = Retrofit.Builder()
                    .baseUrl(serverUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    
                val apiService = retrofit.create(PosApiService::class.java)
                val repository = OrderRepository(apiService)
                
                val viewModel: KitchenViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(onUrlSet: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("KDS Setup") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Server IP Address",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Example: 192.168.1.5:8000",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Server Address") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                singleLine = true
            )
            
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onUrlSet(text.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Connect", fontSize = 18.sp)
            }
        }
    }
}

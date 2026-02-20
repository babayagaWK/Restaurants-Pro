package com.example.restaurantpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // TODO: Replace with your actual backend URL URL like "https://yourusername.pythonanywhere.com/"
        val retrofit = Retrofit.Builder()
            .baseUrl("https://kds-pos.com/") // Default dummy URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        val apiService = retrofit.create(PosApiService::class.java)
        val repository = OrderRepository(apiService)
        
        setContent {
            val viewModel: KitchenViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return KitchenViewModel(repository) as T
                    }
                }
            )
            KitchenScreen(viewModel = viewModel)
        }
    }
}

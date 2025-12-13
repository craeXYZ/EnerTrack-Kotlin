package com.enertrack.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.SessionManager
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.AuthRepository

// 1. Factory butuh Context untuk diteruskan ke Repository
class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {

            // 2. Siapkan dependensi
            val apiService = RetrofitClient.getInstance(context)
            val sessionManager = SessionManager(context)

            // 3. UPDATED: Masukkan context sebagai parameter ke-3
            val repository = AuthRepository(apiService, sessionManager, context)

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
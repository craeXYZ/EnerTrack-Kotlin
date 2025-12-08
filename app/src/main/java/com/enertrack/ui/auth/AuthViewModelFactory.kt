package com.enertrack.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.SessionManager
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.AuthRepository

// 1. Factory sekarang butuh Context untuk bekerja
class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {

            // 2. Di dalam factory, kita buat semua yang dibutuhkan oleh AuthViewModel
            val apiService = RetrofitClient.getInstance(context)
            val sessionManager = SessionManager(context)
            val repository = AuthRepository(apiService, sessionManager) // Berikan sessionManager ke repository

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
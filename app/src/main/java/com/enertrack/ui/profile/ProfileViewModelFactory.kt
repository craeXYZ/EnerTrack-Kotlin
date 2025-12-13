package com.enertrack.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.SessionManager
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.AuthRepository

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {

            // Setup dependensi yang dibutuhkan
            val apiService = RetrofitClient.getInstance(context)
            val sessionManager = SessionManager(context)

            // Pastikan AuthRepository dibuat dengan context (untuk hapus DB)
            val authRepository = AuthRepository(apiService, sessionManager, context)

            return ProfileViewModel(authRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
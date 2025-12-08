package com.enertrack.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.SessionManager

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val sessionManager = SessionManager(context)
            @Suppress("UNCHECKED_CAST")
            // Pass context dan sessionManager sesuai konstruktor ViewModel
            return ProfileViewModel(context, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
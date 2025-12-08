package com.enertrack.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.User
import com.enertrack.data.network.RetrofitClient
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _logoutStatus = MutableLiveData<Boolean?>()
    val logoutStatus: LiveData<Boolean?> = _logoutStatus

    private val _updateStatus = MutableLiveData<Boolean?>()
    val updateStatus: LiveData<Boolean?> = _updateStatus

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        viewModelScope.launch {
            // Ambil semua data dari Session Manager
            val userId = sessionManager.getUserId() ?: "0" // Default "0" kalau belum login
            val username = sessionManager.getUsername()
            val email = sessionManager.getEmail()

            // === PERBAIKAN: Masukkan uid dan image ===
            _userData.value = User(
                uid = userId,       // UID: Ada (dari session atau default)
                username = username,
                email = email,
                image = null        // Image: Ngga ada (di-set null)
            )
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            try {
                sessionManager.clearSession()
                RetrofitClient.clearCookies(context)
                _logoutStatus.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _logoutStatus.value = true
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            // Simulasi update
            kotlinx.coroutines.delay(1000)
            sessionManager.saveUsername(newUsername)
            fetchUserData()
            _updateStatus.value = true
        }
    }

    fun resetLogoutStatus() {
        _logoutStatus.value = null
    }

    fun resetUpdateStatus() {
        _updateStatus.value = null
    }
}

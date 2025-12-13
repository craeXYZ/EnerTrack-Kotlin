package com.enertrack.ui.profile

import androidx.lifecycle.*
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.User
import com.enertrack.data.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _logoutStatus = MutableLiveData<Boolean>()
    val logoutStatus: LiveData<Boolean> = _logoutStatus

    private val _updateStatus = MutableLiveData<Boolean?>()
    val updateStatus: LiveData<Boolean?> = _updateStatus

    init {
        refreshUserData()
    }

    fun refreshUserData() {
        viewModelScope.launch {
            // Ambil data user dari SessionManager
            // Kita pakai Flow biar reaktif, atau getter biasa
            val username = sessionManager.usernameFlow.firstOrNull() ?: "User"
            val email = sessionManager.emailFlow.firstOrNull() ?: "Email"

            // Update UI dengan data dummy uid/image karena data utama ada di username/email
            _userData.value = User(uid = "", email = email, username = username, image = null)
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            // Simpan ke lokal
            sessionManager.saveUsername(newUsername)
            refreshUserData()
            _updateStatus.value = true
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = null
    }

    // === INI BAGIAN PALING PENTING ===
    fun logoutUser() {
        viewModelScope.launch {
            try {
                // 1. Panggil Repository untuk HAPUS DATABASE & COOKIES
                // Inilah yang kemarin hilang, makanya log tidak muncul
                authRepository.logout()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 2. Beritahu UI untuk pindah halaman (mau sukses/gagal tetap logout)
                _logoutStatus.value = true
            }
        }
    }
}
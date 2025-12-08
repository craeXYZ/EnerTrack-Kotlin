package com.enertrack.ui.splash // Sesuaikan package

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
// Import model Appliance jika belum ada
import com.enertrack.data.model.Appliance
import com.enertrack.data.network.ApiService
import com.enertrack.data.network.RetrofitClient // Import RetrofitClient
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

// Status login (tetap sama)
enum class LoginStatus {
    CHECKING,
    LOGGED_IN,
    LOGGED_OUT
}

// ViewModel
class SplashViewModel(private val apiService: ApiService) : ViewModel() {

    private val _loginStatus = MutableLiveData<LoginStatus>(LoginStatus.CHECKING) // Mulai dengan status checking
    val loginStatus: LiveData<LoginStatus> = _loginStatus

    init {
        checkUserLoginStatus()
    }

    private fun checkUserLoginStatus() {
        viewModelScope.launch {
            try {
                // --- PERUBAHAN DI SINI ---
                // Panggil endpoint getUserAppliances() untuk cek login
                Log.d("SplashViewModel", "Attempting to call getUserAppliances for login check...")
                val response = apiService.getUserAppliances() // <-- Ganti getProfile() jadi getUserAppliances()

                if (response.isSuccessful) {
                    // Jika API sukses (2xx), berarti user sudah login (cookie valid)
                    // Tidak peduli response.body() nya null atau tidak, yang penting sukses
                    Log.d("SplashViewModel", "Login check successful (getUserAppliances returned ${response.code()}), navigating to Main")
                    _loginStatus.postValue(LoginStatus.LOGGED_IN)
                } else {
                    // Jika API gagal (misal 401 Unauthorized), user belum login
                    Log.d("SplashViewModel", "Login check failed (API Error: ${response.code()}), navigating to Welcome")
                    _loginStatus.postValue(LoginStatus.LOGGED_OUT)
                }
            } catch (e: Exception) {
                // Jika terjadi error (misal masalah jaringan), anggap belum login
                Log.e("SplashViewModel", "Login check failed (Exception during API call)", e)
                _loginStatus.postValue(LoginStatus.LOGGED_OUT)
            }
        }
    }
}

// Factory (tetap sama)
class SplashViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


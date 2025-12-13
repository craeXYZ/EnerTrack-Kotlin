package com.enertrack.ui.auth

import androidx.lifecycle.*
import com.enertrack.data.model.User
import com.enertrack.data.repository.AuthRepository
// Import UIState dan helper Result yang sudah kita buat
import com.enertrack.util.UIState
// Asumsi ekstensi onSuccess/onFailure ada di file repository atau utils
import com.enertrack.data.repository.onSuccess
import com.enertrack.data.repository.onFailure
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<UIState<User>>()
    val loginResult: LiveData<UIState<User>> = _loginResult

    private val _registerResult = MutableLiveData<UIState<Unit>>()
    val registerResult: LiveData<UIState<Unit>> = _registerResult

    // === TAMBAHAN: LiveData untuk status Logout ===
    private val _logoutResult = MutableLiveData<UIState<Unit>>()
    val logoutResult: LiveData<UIState<Unit>> = _logoutResult

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _loginResult.value = UIState.Loading
            authRepository.login(email, pass)
                .onSuccess { user ->
                    _loginResult.value = UIState.Success(user)
                }
                .onFailure { exception ->
                    _loginResult.value = UIState.Error(exception.message ?: "An unknown error occurred")
                }
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _registerResult.value = UIState.Loading
            authRepository.register(name, email, pass)
                .onSuccess {
                    _registerResult.value = UIState.Success(Unit)
                }
                .onFailure { exception ->
                    _registerResult.value = UIState.Error(exception.message ?: "An unknown error occurred")
                }
        }
    }

    // === UPDATED: FUNGSI LOGOUT ===
    fun logout() {
        viewModelScope.launch {
            _logoutResult.value = UIState.Loading
            try {
                // 1. Panggil repository untuk bersihkan cookie/session
                authRepository.logout()

                // 2. Beritahu UI kalau logout sukses
                _logoutResult.value = UIState.Success(Unit)
            } catch (e: Exception) {
                // Logout lokal jarang gagal, tapi tetap kita handle
                _logoutResult.value = UIState.Error(e.message ?: "Logout failed")
            }
        }
    }
}
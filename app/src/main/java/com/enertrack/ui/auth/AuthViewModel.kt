package com.enertrack.ui.auth

import androidx.lifecycle.*
import com.enertrack.data.model.User
import com.enertrack.data.repository.AuthRepository
// Import UIState dan helper Result yang sudah kita buat
import com.enertrack.util.UIState
import com.enertrack.data.repository.onSuccess
import com.enertrack.data.repository.onFailure
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<UIState<User>>()
    val loginResult: LiveData<UIState<User>> = _loginResult

    private val _registerResult = MutableLiveData<UIState<Unit>>()
    val registerResult: LiveData<UIState<Unit>> = _registerResult

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            // Beri tahu UI kalau proses sedang loading
            _loginResult.value = UIState.Loading
            authRepository.login(email, pass)
                .onSuccess { user ->
                    // Beri tahu UI kalau proses sukses dan kirim datanya
                    _loginResult.value = UIState.Success(user)
                }
                .onFailure { exception ->
                    // Beri tahu UI kalau proses gagal dan kirim pesan errornya
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
}
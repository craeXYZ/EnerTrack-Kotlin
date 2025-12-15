package com.enertrack.ui.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.enertrack.data.model.DeviceOption
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.Result
import kotlinx.coroutines.launch

class AiChatViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val _deviceOptions = MutableLiveData<List<DeviceOption>>()
    val deviceOptions: LiveData<List<DeviceOption>> = _deviceOptions

    private val _isAiTyping = MutableLiveData<Boolean>()
    val isAiTyping: LiveData<Boolean> = _isAiTyping

    private val _aiResponse = MutableLiveData<String>()
    val aiResponse: LiveData<String> = _aiResponse

    // [FIX] Ubah jadi nullable biar bisa di-reset
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchDeviceOptions()
    }

    private fun fetchDeviceOptions() {
        viewModelScope.launch {
            when (val result = repository.getDeviceOptions()) {
                is Result.Success -> {
                    _deviceOptions.value = result.data
                }
                is Result.Failure -> {
                    _deviceOptions.value = listOf(DeviceOption("Pilih Perangkat (Umum)", ""))
                }
                else -> {}
            }
        }
    }

    fun sendMessage(userMessage: String, deviceContext: String) {
        viewModelScope.launch {
            _isAiTyping.value = true
            _errorMessage.value = null // Reset error pas mulai kirim

            when (val result = repository.sendChatToAi(userMessage, deviceContext)) {
                is Result.Success -> {
                    _aiResponse.value = result.data
                }
                is Result.Failure -> {
                    _errorMessage.value = "Gagal terhubung ke AI. Cek koneksi internetmu."
                    _aiResponse.value = "Maaf, saya sedang pusing (Error Server). Coba tanya lagi nanti ya."
                }
                else -> {}
            }

            _isAiTyping.value = false
        }
    }

    // [FIX] Fungsi reset error manual
    fun resetErrorState() {
        _errorMessage.value = null
    }
}

class AiChatViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
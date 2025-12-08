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

    // Data List Perangkat (Buat Spinner)
    private val _deviceOptions = MutableLiveData<List<DeviceOption>>()
    val deviceOptions: LiveData<List<DeviceOption>> = _deviceOptions

    // Status Loading (Buat nunjukin "AI sedang mengetik...")
    private val _isAiTyping = MutableLiveData<Boolean>()
    val isAiTyping: LiveData<Boolean> = _isAiTyping

    // Jawaban dari AI
    private val _aiResponse = MutableLiveData<String>()
    val aiResponse: LiveData<String> = _aiResponse

    // Error message (kalau server down/no internet)
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchDeviceOptions()
    }

    // 1. Ambil Data Perangkat dari Backend
    private fun fetchDeviceOptions() {
        viewModelScope.launch {
            when (val result = repository.getDeviceOptions()) {
                is Result.Success -> {
                    // Masukkan data asli dari server
                    _deviceOptions.value = result.data
                }
                is Result.Failure -> { // Changed from Error to Failure
                    // Kalau error/kosong, kasih opsi default biar gak crash
                    _deviceOptions.value = listOf(DeviceOption("Pilih Perangkat (Umum)", ""))
                    // _errorMessage.value = "Gagal memuat perangkat: ${result.exception.message}"
                }
                else -> {}
            }
        }
    }

    // 2. Kirim Pesan ke AI
    fun sendMessage(userMessage: String, deviceContext: String) {
        viewModelScope.launch {
            _isAiTyping.value = true
            _errorMessage.value = null

            when (val result = repository.sendChatToAi(userMessage, deviceContext)) {
                is Result.Success -> {
                    _aiResponse.value = result.data // Jawaban sukses dari Gemini
                }
                is Result.Failure -> { // Changed from Error to Failure
                    _errorMessage.value = "Gagal terhubung ke AI. Cek koneksi internetmu."
                    _aiResponse.value = "Maaf, saya sedang pusing (Error Server). Coba tanya lagi nanti ya."
                }
                else -> {}
            }

            _isAiTyping.value = false
        }
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
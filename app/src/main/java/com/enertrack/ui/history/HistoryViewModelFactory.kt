package com.enertrack.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.EnerTrackDatabase // <-- 1. IMPORT DATABASE
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.HistoryRepository

// Factory sekarang butuh Context untuk bekerja
class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Factory ini hanya tahu cara membuat HistoryViewModel
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {

            // 1. Gunakan context untuk membuat ApiService
            val apiService = RetrofitClient.getInstance(context)

            // --- INI BAGIAN YANG DIPERBAIKI ---

            // 2. DAPETIN DATABASE-NYA
            val database = EnerTrackDatabase.getDatabase(context)

            // 3. AMBIL DAO DARI DATABASE
            val historyDao = database.historyDao()

            // 4. BUAT REPOSITORY DENGAN RESEP BARU (tambahin historyDao)
            val repository = HistoryRepository(apiService, historyDao)

            @Suppress("UNCHECKED_CAST")
            // 5. Berikan repository ke ViewModel
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

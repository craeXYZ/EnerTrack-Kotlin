package com.enertrack.ui.calculate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.EnerTrackDatabase // <-- 1. IMPORT DATABASE
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.CalculateRepository
import com.enertrack.data.repository.HistoryRepository

class CalculateViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculateViewModel::class.java)) {

            val apiService = RetrofitClient.getInstance(context)

            // --- INI BAGIAN YANG DIPERBAIKI ---

            // 2. DAPETIN DATABASE-NYA
            val database = EnerTrackDatabase.getDatabase(context)

            // 3. AMBIL DAO DARI DATABASE
            val historyDao = database.historyDao()

            // 4. BUAT REPOSITORY DENGAN RESEP BARU (tambahin historyDao)
            val historyRepository = HistoryRepository(apiService, historyDao)

            // (CalculateRepository biarin dulu, nanti kita update juga kalo perlu)
            val calculateRepository = CalculateRepository(apiService)

            @Suppress("UNCHECKED_CAST")
            // BERIKAN ALAT BARU KE VIEWMODEL
            return CalculateViewModel(calculateRepository, historyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

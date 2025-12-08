package com.enertrack.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.local.EnerTrackDatabase
import com.enertrack.data.local.SessionManager
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.StatisticsRepository // <-- 1. IMPORT BARU

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            val apiService = RetrofitClient.getInstance(context)
            val sessionManager = SessionManager(context)

            val database = EnerTrackDatabase.getDatabase(context)
            val historyDao = database.historyDao()
            val historyRepository = HistoryRepository(apiService, historyDao)

            // --- 2. INI BAGIAN YANG DITAMBAH ---
            // Kita buat StatisticsRepository, sama kayak di AnalyticsViewModelFactory
            val statsRepository = StatisticsRepository(apiService)
            // ------------------------------------

            @Suppress("UNCHECKED_CAST")
            // --- 3. KASIH statsRepository KE VIEWMODEL ---
            return HomeViewModel(
                historyRepository,
                sessionManager,
                statsRepository // <-- INI DIA
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


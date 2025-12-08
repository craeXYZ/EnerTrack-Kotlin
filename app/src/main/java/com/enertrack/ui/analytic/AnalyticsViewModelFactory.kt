package com.enertrack.ui.analytic

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enertrack.data.network.RetrofitClient
import com.enertrack.data.repository.StatisticsRepository

class AnalyticsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {

            // Siapkan repository yang dibutuhkan oleh AnalyticsViewModel
            val repository = StatisticsRepository(RetrofitClient.getInstance(context))

            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.enertrack.ui.analytic

import androidx.lifecycle.*
import com.enertrack.data.model.CategoryChartData
import com.enertrack.data.model.ChartDataPoint
import com.enertrack.data.repository.StatisticsRepository
import com.enertrack.data.repository.onFailure
import com.enertrack.data.repository.onSuccess
import com.enertrack.util.UIState
import kotlinx.coroutines.launch

class AnalyticsViewModel(private val statsRepository: StatisticsRepository) : ViewModel() {

    // LiveData untuk setiap grafik, dibungkus UIState
    private val _weeklyStats = MutableLiveData<UIState<List<ChartDataPoint>>>()
    val weeklyStats: LiveData<UIState<List<ChartDataPoint>>> = _weeklyStats

    private val _monthlyStats = MutableLiveData<UIState<List<ChartDataPoint>>>()
    val monthlyStats: LiveData<UIState<List<ChartDataPoint>>> = _monthlyStats

    private val _categoryStats = MutableLiveData<UIState<List<CategoryChartData>>>()
    val categoryStats: LiveData<UIState<List<CategoryChartData>>> = _categoryStats

    // Fungsi ini akan dipanggil oleh Fragment untuk memuat semua data
    fun fetchAllStatistics() {
        fetchWeeklyStats()
        fetchMonthlyStats()
        fetchCategoryStats()
    }

    private fun fetchWeeklyStats(date: String? = null) {
        viewModelScope.launch {
            _weeklyStats.value = UIState.Loading
            statsRepository.getWeeklyStatistics(date)
                .onSuccess { data ->
                    _weeklyStats.value = UIState.Success(data)
                }
                .onFailure { error ->
                    _weeklyStats.value = UIState.Error(error.message ?: "Failed to load weekly data")
                }
        }
    }

    private fun fetchMonthlyStats() {
        viewModelScope.launch {
            _monthlyStats.value = UIState.Loading
            statsRepository.getMonthlyStatistics()
                .onSuccess { data ->
                    _monthlyStats.value = UIState.Success(data)
                }
                .onFailure { error ->
                    _monthlyStats.value = UIState.Error(error.message ?: "Failed to load monthly data")
                }
        }
    }

    private fun fetchCategoryStats() {
        viewModelScope.launch {
            _categoryStats.value = UIState.Loading
            statsRepository.getCategoryStatistics()
                .onSuccess { data ->
                    _categoryStats.value = UIState.Success(data)
                }
                .onFailure { error ->
                    _categoryStats.value = UIState.Error(error.message ?: "Failed to load category data")
                }
        }
    }
}
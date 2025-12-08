package com.enertrack.ui.home

import android.util.Log
import androidx.lifecycle.*
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.ChartDataPoint
import com.enertrack.data.model.HistoryItem
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.Result
import com.enertrack.data.repository.StatisticsRepository
import com.enertrack.data.repository.onFailure
import com.enertrack.data.repository.onSuccess
import com.enertrack.util.UIState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val historyRepository: HistoryRepository,
    private val sessionManager: SessionManager,
    private val statsRepository: StatisticsRepository
) : ViewModel() {

    val isLoading = MutableLiveData<Boolean>()
    val username = MutableLiveData<String>()
    val todayKwh = MutableLiveData<Double>()
    val weeklyKwh = MutableLiveData<Double>()
    val monthlyCost = MutableLiveData<Double>()
    val recentHistory = MutableLiveData<List<HistoryItem>>()

    private val _weeklyChartData = MutableLiveData<UIState<List<ChartDataPoint>>>()
    val weeklyChartData: LiveData<UIState<List<ChartDataPoint>>> = _weeklyChartData

    init {
        observeHistoryDatabase()
        checkSessionAndFetchData()
    }

    private fun checkSessionAndFetchData() {
        viewModelScope.launch {
            try {
                val user = sessionManager.usernameFlow.first { !it.isNullOrBlank() }
                Log.d("HomeViewModel", "Session ready for user: $user. Fetching data...")
                onRefresh()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to get session before refresh", e)
                isLoading.value = false
            }
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            isLoading.value = true
            username.value = sessionManager.usernameFlow.first() ?: "User"

            // Panggil sync dan chart
            val syncResult = historyRepository.syncHistoryFromServer()
            fetchWeeklyChartData()

            if (syncResult is Result.Failure) {
                isLoading.value = false
            }
        }
    }

    private fun observeHistoryDatabase() {
        viewModelScope.launch {
            historyRepository.getHistoryList().collectLatest { items ->
                if (items.isNotEmpty()) {
                    val latestDate = items.first().date
                    val latestSubmissionItems = items.filter { it.date == latestDate }

                    var totalDaily = 0.0
                    val tarifPerKwh = 1444.70

                    latestSubmissionItems.forEach { item ->
                        // ================== PERBAIKAN ERROR DI SINI ==================
                        // Kasih default 0.0 kalo datanya null
                        val power = item.power ?: 0.0
                        val usage = item.usage ?: 0.0
                        val calculatedDailyKwh = (power * usage) / 1000.0
                        totalDaily += calculatedDailyKwh
                        // =============================================================
                    }

                    val totalWeekly = totalDaily * 7
                    val totalMonthly = totalDaily * 30
                    val totalCost = totalMonthly * tarifPerKwh

                    todayKwh.value = totalDaily
                    weeklyKwh.value = totalWeekly
                    monthlyCost.value = totalCost
                    recentHistory.value = latestSubmissionItems.take(3)
                } else {
                    setEmptyState()
                }
                isLoading.value = false
            }
        }
    }

    private fun fetchWeeklyChartData(date: String? = null) {
        viewModelScope.launch {
            _weeklyChartData.value = UIState.Loading
            statsRepository.getWeeklyStatistics(date)
                .onSuccess { data ->
                    _weeklyChartData.value = UIState.Success(data)
                }
                .onFailure { error ->
                    _weeklyChartData.value = UIState.Error(error.message ?: "Failed to load weekly data")
                }
        }
    }

    private fun setEmptyState() {
        todayKwh.value = 0.0
        weeklyKwh.value = 0.0
        monthlyCost.value = 0.0
        recentHistory.value = emptyList()
    }
}


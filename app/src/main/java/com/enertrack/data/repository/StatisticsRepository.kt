package com.enertrack.data.repository

import com.enertrack.data.model.CategoryChartData
import com.enertrack.data.model.ChartDataPoint
import com.enertrack.data.network.ApiService
// Import Result yang benar
import com.enertrack.data.repository.Result

class StatisticsRepository(private val apiService: ApiService) {

    suspend fun getWeeklyStatistics(date: String?): Result<List<ChartDataPoint>> {
        return try {
            val response = apiService.getWeeklyStatistics(date)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(Exception("Failed to fetch weekly stats: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun getMonthlyStatistics(): Result<List<ChartDataPoint>> {
        return try {
            val response = apiService.getMonthlyStatistics()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(Exception("Failed to fetch monthly stats: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun getCategoryStatistics(): Result<List<CategoryChartData>> {
        return try {
            val response = apiService.getCategoryStatistics()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(Exception("Failed to fetch category stats: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}
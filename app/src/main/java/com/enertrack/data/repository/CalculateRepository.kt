package com.enertrack.data.repository

import com.enertrack.data.model.AnalysisResult
import com.enertrack.data.model.AnalyzePayload
import com.enertrack.data.model.Category
import com.enertrack.data.model.SubmitPayload
import com.enertrack.data.model.SubmitResponseData
import com.enertrack.data.network.ApiService
import com.enertrack.data.model.DeviceResponse
import com.enertrack.data.repository.Result

class CalculateRepository(private val apiService: ApiService) {

    suspend fun getCategories(): Result<List<Category>> {
        return try {
            val response = apiService.getCategories()
            if (response.isSuccessful) {
                // PERBAIKAN: Menggunakan Result.Success (huruf S besar)
                Result.Success(response.body() ?: emptyList())
            } else {
                // PERBAIKAN: Menggunakan Result.Failure (huruf F besar)
                Result.Failure(Exception("Failed to fetch categories: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun getBrands(): Result<List<String>> {
        return try {
            val response = apiService.getBrands()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(Exception("Failed to fetch brands: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun getHouseCapacities(): Result<List<String>> {
        return try {
            val response = apiService.getHouseCapacities()
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(Exception("Failed to fetch house capacities: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun getDevicesByBrand(brand: String): Result<List<DeviceResponse>> {
        return try {
            val response = apiService.getDevicesByBrand(brand)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Failure(Exception("Failed to fetch devices"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun submitDevices(payload: SubmitPayload): Result<SubmitResponseData> {
        return try {
            val response = apiService.submitDevices(payload)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Failure(Exception("Failed to submit devices: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun analyzeDevices(payload: AnalyzePayload): Result<AnalysisResult> {
        return try {
            val response = apiService.analyzeDevices(payload)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Failure(Exception("Failed to analyze devices: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}
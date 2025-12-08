package com.enertrack.data.repository

import com.enertrack.data.model.Appliance
import com.enertrack.data.network.ApiService
// Pastikan baris import ini ada! Ini yang menghubungkan ke Result.kt
import com.enertrack.data.repository.Result

class ApplianceRepository(private val apiService: ApiService) {

    suspend fun getUserAppliances(): Result<List<Appliance>> {
        return try {
            val response = apiService.getUserAppliances()
            if (response.isSuccessful) {
                // Memanggil Result.Success yang sudah kita definisikan
                Result.Success(response.body() ?: emptyList())
            } else {
                // Memanggil Result.Failure yang sudah kita definisikan
                Result.Failure(Exception("Failed to fetch appliances"))
            }
        } catch (e: Exception) {
            // Memanggil Result.Failure yang sudah kita definisikan
            Result.Failure(e)
        }
    }
}
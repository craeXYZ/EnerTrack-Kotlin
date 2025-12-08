package com.enertrack.data.repository

import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.User
import com.enertrack.data.network.ApiService

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    suspend fun login(email: String, pass: String): Result<User> {
        return try {
            val response = apiService.login(mapOf("email" to email, "password" to pass))
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!

                // ================== PERBAIKAN HEADER COOKIE ==================
                // Cek kedua kemungkinan casing header biar lebih robust
                val cookie = response.headers()["set-cookie"] ?: response.headers()["Set-Cookie"]
                // =============================================================

                if (cookie != null) {
                    sessionManager.saveAuthToken(cookie)
                }

                if (user.username != null) {
                    sessionManager.saveUsername(user.username)
                }

                if (user.email != null) {
                    sessionManager.saveEmail(user.email)
                }

                Result.Success(user)
            } else {
                // Ambil pesan error dari body error kalau ada
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.Failure(Exception("Login failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun register(name: String, email: String, pass: String): Result<Unit> {
        return try {
            // ================== KODE YANG SUDAH BENAR ==================
            // Pastikan tetap pakai "username"
            val requestMap = mapOf(
                "username" to name,
                "email" to email,
                "password" to pass
            )
            // =======================================================

            val response = apiService.register(requestMap)

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.Failure(Exception("Registration failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun isUserLoggedIn(): Boolean {
        return sessionManager.getAuthToken() != null
    }
}
package com.enertrack.data.repository

import android.content.Context
import android.util.Log // Import Log buat debugging
import com.enertrack.data.local.EnerTrackDatabase
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.User
import com.enertrack.data.network.ApiService
import com.enertrack.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val context: Context
) {

    suspend fun login(email: String, pass: String): Result<User> {
        return try {
            val response = apiService.login(mapOf("email" to email, "password" to pass))
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!

                val cookie = response.headers()["set-cookie"] ?: response.headers()["Set-Cookie"]

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
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.Failure(Exception("Login failed: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    suspend fun register(name: String, email: String, pass: String): Result<Unit> {
        return try {
            val requestMap = mapOf(
                "username" to name,
                "email" to email,
                "password" to pass
            )

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
        withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Starting logout process...")

                // 1. Bersihkan Session & Cookie
                sessionManager.clearSession()
                RetrofitClient.clearCookies(context)
                Log.d("AuthRepository", "Session and cookies cleared.")

                // 2. KOSONGKAN ISI DATABASE
                val db = EnerTrackDatabase.getDatabase(context)
                if (db.isOpen) {
                    db.clearAllTables()
                    Log.d("AuthRepository", "Database tables cleared via Room.")
                }

            } catch (e: Exception) {
                Log.e("AuthRepository", "Error during logout cleanup: ${e.message}")
                e.printStackTrace()

                // === FALLBACK: NUCLEAR OPTION ===
                // Kalau clearAllTables gagal (misal karena DB locked),
                // Kita hapus file database fisiknya secara paksa.
                try {
                    val dbName = "enertrack_database" // Sesuaikan nama DB di EnerTrackDatabase.kt
                    context.deleteDatabase(dbName)
                    Log.d("AuthRepository", "Fallback: Database file deleted physically.")
                } catch (ex: Exception) {
                    Log.e("AuthRepository", "Fallback failed: ${ex.message}")
                }
            }
        }
    }

    suspend fun isUserLoggedIn(): Boolean {
        return sessionManager.getAuthToken() != null
    }
}
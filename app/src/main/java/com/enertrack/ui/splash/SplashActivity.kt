package com.enertrack.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.enertrack.MainActivity
import com.enertrack.R
import com.enertrack.data.local.SessionManager
import com.enertrack.ui.auth.WelcomeActivity // Pastikan ini adalah activity tujuan login
import kotlinx.coroutines.runBlocking // Perlu import ini karena SessionManager pakai runBlocking

// Kami menyederhanakan SplashActivity untuk menghilangkan potensi race condition dari ViewModel/Flow.
// Menggunakan SessionManager.getUserId() secara synchronous di thread utama (dengan Handler)
// adalah cara paling stabil untuk Splash Screen.

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private val TAG = "SPLASH_CHECK"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sessionManager = SessionManager(this)

        val splashDuration = 2000L // 2 detik

        // Pindah ke screen selanjutnya setelah durasi splash
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, splashDuration)
    }

    private fun checkLoginStatus() {
        // Karena SessionManager.getUserId() menggunakan runBlocking, memanggilnya di sini
        // setelah delay aman dan akan mendapatkan nilai terakhir dari DataStore.
        val userId = sessionManager.getUserId()
        val username = sessionManager.getUsername()
        // Cek token juga untuk validasi ganda
        val authToken = runBlocking { sessionManager.getAuthToken() }

        Log.d(TAG, "=== DIAGNOSA SESI DI SPLASH ===")
        Log.d(TAG, "🆔 User ID (dari DS): '$userId'") // Jika kosong, harusnya ""
        Log.d(TAG, "👤 Username (dari DS): '$username'")
        Log.d(TAG, "🔑 Auth Token (dari DS): '${authToken?.take(8)}...'") // Sensor token sebagian

        // Logika Pengecekan Paling Ketat:
        // 1. User ID tidak boleh null
        // 2. User ID tidak boleh string kosong "" (ini hasil dari perbaikan SessionManager)
        // 3. User ID tidak boleh "0" (asumsi 0 adalah ID default tidak valid)
        // 4. Kita juga bisa cek token untuk lebih yakin: authToken juga tidak boleh null/kosong
        val isUserIdValid = !userId.isNullOrEmpty() && userId != "0"
        val isTokenValid = !authToken.isNullOrEmpty()

        // Cukup cek ID, karena token seharusnya otomatis ada kalau ID ada
        val isLoggedIn = isUserIdValid

        val targetActivity = if (isLoggedIn) {
            Log.i(TAG, "STATUS: ✅ User ID '$userId' valid. Pindah ke MainActivity.")
            MainActivity::class.java // Pindah ke Home
        } else {
            Log.i(TAG, "STATUS: ❌ Sesi kosong/invalid. Pindah ke WelcomeActivity.")
            WelcomeActivity::class.java // Pindah ke Welcome/Auth
        }

        // Pindah ke Activity tujuan
        val intent = Intent(this, targetActivity)
        // Flag ini penting agar Home Activity menjadi root dan Splash ditutup.
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
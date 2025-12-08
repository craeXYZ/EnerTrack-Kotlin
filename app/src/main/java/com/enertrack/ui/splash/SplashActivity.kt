package com.enertrack.ui.splash // Pastikan package ini benar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
// Hapus Handler dan Looper, kita pakai ViewModel
// import android.os.Handler
// import android.os.Looper
import androidx.activity.viewModels // Butuh import ini
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer // Butuh import ini
import com.enertrack.MainActivity // Import MainActivity
import com.enertrack.R
// Import ViewModel dan Factory
import com.enertrack.ui.splash.SplashViewModel
import com.enertrack.ui.splash.SplashViewModelFactory
import com.enertrack.ui.splash.LoginStatus
// Import RetrofitClient untuk mendapatkan ApiService
import com.enertrack.data.network.RetrofitClient
// Import Activity tujuan
import com.enertrack.ui.auth.WelcomeActivity // Sesuaikan jika nama Activity login/welcome beda


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // Inisialisasi ViewModel menggunakan Factory
    private val splashViewModel: SplashViewModel by viewModels {
        // Ambil instance ApiService dari RetrofitClient
        SplashViewModelFactory(RetrofitClient.getInstance(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Set layout splash screen (udah bener)

        // Amati perubahan status login dari ViewModel
        splashViewModel.loginStatus.observe(this, Observer { status ->
            // Jangan langsung pindah, tunggu statusnya jelas (LOGGED_IN / LOGGED_OUT)
            when (status) {
                LoginStatus.LOGGED_IN -> navigateToMain()
                LoginStatus.LOGGED_OUT -> navigateToWelcome()
                LoginStatus.CHECKING -> { /* Biarkan splash screen tampil selagi mengecek */ }
                null -> navigateToWelcome() // Default ke welcome jika null (misal ada error tak terduga)
            }
        })

        // HAPUS Handler 3 detik yang lama
        /*
        Handler(Looper.getMainLooper()).postDelayed({
            // UBAH TUJUAN INTENT KE WelcomeActivity
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish() // Tutup SplashActivity agar tidak bisa kembali ke sini
        }, 3000) // Delay 3 detik
        */
    }

    // Fungsi navigasi ke MainActivity (jika sudah login)
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        // Tambahkan flag agar MainActivity jadi root baru, menghapus Splash dari back stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        // finish() tidak perlu karena CLEAR_TASK sudah menghapusnya
    }

    // Fungsi navigasi ke WelcomeActivity (jika belum login)
    private fun navigateToWelcome() {
        val intent = Intent(this, WelcomeActivity::class.java) // Ganti ke LoginActivity jika perlu
        // Tambahkan flag agar WelcomeActivity jadi root baru
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        // finish() tidak perlu
    }
}


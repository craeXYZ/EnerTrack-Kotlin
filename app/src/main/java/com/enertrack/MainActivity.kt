package com.enertrack

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.FcmTokenRequest
import com.enertrack.data.network.ApiService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var appBarLayout: AppBarLayout

    // 1. SessionManager
    private lateinit var sessionManager: SessionManager

    // --- Inisialisasi API Service ---
    private val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://backend-enertrack-production.up.railway.app/") // URL Production
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifikasi Diizinkan! IoT Alert aktif.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifikasi Ditolak.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi SessionManager
        sessionManager = SessionManager(this)

        // 1. Setup View
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        bottomNavView = findViewById(R.id.bottom_nav_view)
        appBarLayout = findViewById(R.id.app_bar_layout)

        setSupportActionBar(toolbar)
        navController = navHostFragment.navController

        // 2. Setup Navigation
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_analytics, R.id.navigation_calculate, R.id.navigation_history)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)

        // 3. Logic Hide BottomNav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_profile -> {
                    appBarLayout.isVisible = false
                    bottomNavView.isVisible = false
                }
                else -> {
                    appBarLayout.isVisible = true
                    bottomNavView.isVisible = true
                }
            }
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    navController.navigate(R.id.navigation_profile)
                    true
                }
                else -> false
            }
        }

        // 4. Ask Permission
        askNotificationPermission()

        // 5. Cek Login & Kirim Token (Pakai Coroutine biar aman dari Race Condition)
        lifecycleScope.launch {
            // Kasih napas dikit 1 detik biar DataStore selesai nulis dari LoginFragment
            delay(1000)
            checkAndSyncToken()
        }
    }

    private fun checkAndSyncToken() {
        // Ambil semua data sesi buat diagnosa
        val userIdString = sessionManager.getUserId()
        val username = sessionManager.getUsername()
        val email = sessionManager.getEmail()

        Log.d("FCM_LOG", "=== DIAGNOSA SESSION ===")
        Log.d("FCM_LOG", "👤 Username: $username")
        Log.d("FCM_LOG", "📧 Email: $email")
        Log.d("FCM_LOG", "🆔 User ID: $userIdString")

        // Konversi ID ke Int
        val userId = userIdString?.toIntOrNull()

        if (userId != null && userId != 0) {
            Log.d("FCM_LOG", "✅ User Login Valid (ID: $userId). OTW sinkron token...")
            getAndSendFcmToken(userId)
        } else {
            if (!username.isNullOrEmpty()) {
                // KASUS KHUSUS: Username ada, tapi ID Null.
                // Ini berarti mapping di User.kt SALAH. JSON 'user_id' gak masuk ke variabel 'uid'.
                Log.e("FCM_LOG", "⚠️ ALARM: Username terbaca ($username) TAPI ID KOSONG!")
                Log.e("FCM_LOG", "👉 Cek file 'User.kt' kamu. Pastikan variabel ID pake @SerializedName(\"user_id\")")
            } else {
                Log.e("FCM_LOG", "❌ Sesi benar-benar kosong. User belum login.")
            }
        }
    }

    private fun getAndSendFcmToken(userId: Int) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_LOG", "Gagal dapet token FCM (Mungkin masalah SHA-1)", task.exception)
                return@OnCompleteListener
            }

            val token = task.result
            Log.d("FCM_LOG", "🔥 Token FCM Dapet: $token")
            sendTokenToBackend(userId, token)
        })
    }

    private fun sendTokenToBackend(userId: Int, token: String) {
        lifecycleScope.launch {
            try {
                // Sekarang userId sudah pasti Int, jadi aman dikirim ke Model
                val request = FcmTokenRequest(userId, token)
                val response = apiService.updateFcmToken(request)

                if (response.isSuccessful) {
                    Log.d("FCM_LOG", "✅ Sukses update token User ID: $userId ke Backend!")
                } else {
                    Log.e("FCM_LOG", "❌ Gagal update token: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("FCM_LOG", "❌ Error koneksi API: ${e.message}")
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Permission already granted
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}
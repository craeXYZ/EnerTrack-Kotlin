package com.enertrack.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.enertrack.MainActivity
import com.enertrack.R
import com.enertrack.data.local.SessionManager
import com.enertrack.data.model.FcmTokenRequest
import com.enertrack.data.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Token baru: $token")

        // Simpan token baru ke server jika user sedang login
        val sessionManager = SessionManager(applicationContext)
        val userIdStr = sessionManager.getUserId()
        val userId = userIdStr?.toIntOrNull()

        if (userId != null && userId != 0) {
            sendTokenToServer(userId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Pesan diterima dari: ${remoteMessage.from}")

        // 1. Cek payload Notifikasi
        remoteMessage.notification?.let {
            showNotification(it.title ?: "EnerTrack", it.body ?: "Pesan baru masuk")
        }

        // 2. Cek payload Data (untuk custom logic dari backend)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "EnerTrack Alert"
            val body = remoteMessage.data["body"] ?: "Periksa status perangkat Anda."
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "enertrack_alert_channel"
        val notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Menggunakan ic_launcher agar aman (fallback dari ic_lightbulb_outline)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "EnerTrack Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(notificationId, builder.build())
    }

    private fun sendTokenToServer(userId: Int, token: String) {
        val apiService = RetrofitClient.getInstance(applicationContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = FcmTokenRequest(userId, token)
                val response = apiService.updateFcmToken(request)
                if (response.isSuccessful) {
                    Log.d("FCM", "Token berhasil diperbarui di server")
                } else {
                    Log.e("FCM", "Gagal update token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error koneksi saat update token", e)
            }
        }
    }
}
package com.enertrack.data.network

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // PERBAIKAN: Hapus trailing slash (/) di akhir BASE_URL
    private const val BASE_URL = "https://project-enertrack-backend-production.up.railway.app"

    private var apiService: ApiService? = null

    // Variable ini menyimpan sesi cookie di memori
    private var cookieJar: PersistentCookieJar? = null

    fun getInstance(context: Context): ApiService {
        if (apiService == null) {
            val cookieCache = SetCookieCache()
            val sharedPrefsCookiePersistor = SharedPrefsCookiePersistor(context.applicationContext)

            cookieJar = PersistentCookieJar(cookieCache, sharedPrefsCookiePersistor)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Default timeout OkHttp cuma 10 detik.
            // AI butuh waktu mikir, jadi kita naikin jadi 60 detik (1 menit).
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar!!)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Waktu maksimal buat nyambung ke server
                .readTimeout(60, TimeUnit.SECONDS)    // Waktu maksimal nunggu jawaban AI
                .writeTimeout(30, TimeUnit.SECONDS)   // Waktu maksimal kirim data
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }

    // === UPDATED: Fungsi Hapus Cookie Total ===
    // Tambahkan parameter context untuk menghapus paksa penyimpanan lokal
    fun clearCookies(context: Context) {
        // 1. Bersihkan session di memori (variable cookieJar di atas)
        cookieJar?.clear()
        cookieJar?.clearSession()

        // 2. HAPUS PAKSA penyimpanan di HP (Shared Preferences)
        // Ini langkah krusial untuk HP fisik agar tidak auto-login akun lama
        val persistor = SharedPrefsCookiePersistor(context.applicationContext)
        persistor.clear()
    }
}
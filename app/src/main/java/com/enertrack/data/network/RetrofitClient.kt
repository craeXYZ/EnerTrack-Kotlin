package com.enertrack.data.network

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://project-enertrack-backend-production.up.railway.app/"

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

            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar!!)
                .addInterceptor(loggingInterceptor)
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

    // === PERBAIKAN DI SINI ===
    // Tambahkan parameter context untuk menghapus paksa penyimpanan lokal
    fun clearCookies(context: Context) {
        // 1. Bersihkan session di memori (kalau ada)
        cookieJar?.clear()
        cookieJar?.clearSession()

        // 2. HAPUS PAKSA penyimpanan di HP (Shared Preferences)
        // Ini memastikan cookie hilang 100% walaupun apiService belum pernah dipanggil
        val persistor = SharedPrefsCookiePersistor(context.applicationContext)
        persistor.clear()
    }
}
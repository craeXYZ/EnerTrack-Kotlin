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

    // FIX: Tambahkan trailing slash (/) di akhir URL. Wajib untuk Retrofit!
    private const val BASE_URL = "https://project-enertrack-backend-production.up.railway.app/"

    private var apiService: ApiService? = null
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
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

    fun clearCookies(context: Context) {
        cookieJar?.clear()
        cookieJar?.clearSession()
        val persistor = SharedPrefsCookiePersistor(context.applicationContext)
        persistor.clear()
    }
}
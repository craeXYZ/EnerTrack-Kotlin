package com.enertrack.data.network

import com.enertrack.data.model.*
import retrofit2.Response
import retrofit2.http.*
import com.enertrack.data.model.DeviceResponse

interface ApiService {

    // == Auth ==
    @POST("login")
    suspend fun login(@Body loginRequest: Map<String, String>): Response<User>

    @POST("register")
    suspend fun register(@Body registerRequest: Map<String, String>): Response<Unit>

    // == Data Utama ==
    @GET("history")
    suspend fun getDeviceHistory(): Response<List<HistoryItem>>

    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    @GET("brands")
    suspend fun getBrands(): Response<List<String>>

    @GET("house-capacity")
    suspend fun getHouseCapacities(): Response<List<String>>

    // == Statistik ==
    @GET("statistics/weekly")
    suspend fun getWeeklyStatistics(@Query("date") date: String? = null): Response<List<ChartDataPoint>>

    @GET("statistics/monthly")
    suspend fun getMonthlyStatistics(): Response<List<ChartDataPoint>>

    @GET("statistics/category")
    suspend fun getCategoryStatistics(): Response<List<CategoryChartData>>

    // == Appliances & Calculate ==
    @GET("user/appliances")
    suspend fun getUserAppliances(): Response<List<Appliance>>

    @POST("user/appliances")
    suspend fun createAppliance(@Body appliance: ApplianceInput): Response<Appliance>

    @HTTP(method = "DELETE", path = "user/appliances", hasBody = true)
    suspend fun deleteAppliance(@Body applianceId: Map<String, Int>): Response<Unit>

    @POST("/submit")
    suspend fun submitCalculation(@Body item: HistoryItem): Response<HistoryItem>

    @POST("submit")
    suspend fun submitDevices(@Body payload: SubmitPayload): Response<SubmitResponseData>

    @POST("analyze")
    suspend fun analyzeDevices(@Body payload: AnalyzePayload): Response<AnalysisResult>

    @GET("api/devices")
    suspend fun getDevicesByBrand(@Query("brand") brand: String): Response<List<DeviceResponse>>

    @HTTP(method = "DELETE", path = "user/appliances", hasBody = true)
    suspend fun deleteHistoryItem(@Body body: Map<String, Int>): Response<Unit>


    // 1. Ambil daftar perangkat unik buat ngisi Dropdown Chat
    @GET("api/devices/list")
    suspend fun getUniqueDevices(): Response<List<DeviceOption>>

    // 2. Kirim Pesan Chat ke AI (+ Konteks Perangkat)
    @POST("api/chat")
    suspend fun sendChat(@Body request: ChatRequest): Response<ChatResponse>

    @GET("api/insight")
    suspend fun getInsight(): Response<InsightResponse>

    // KEMBALI KE RUTE LAMA UNTUK MENGURANGI KEANEHAN DARI PROXY
    @POST("api/user/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Unit>

}
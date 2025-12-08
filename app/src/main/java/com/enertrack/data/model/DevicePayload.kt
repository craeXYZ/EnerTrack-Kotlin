package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

// Ini adalah format untuk setiap "barang" di dalam kardus
data class DevicePayload(
    @SerializedName("jenis_pembayaran")
    val billingType: String,

    @SerializedName("besar_listrik")
    val houseCapacity: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("power")
    val powerRating: Double,

    @SerializedName("duration")
    val dailyUsage: Double,

    @SerializedName("quantity")
    val quantity: Int,

    @SerializedName("category_id")
    val categoryId: Int,

    @SerializedName("brand")
    val brand: String?

)
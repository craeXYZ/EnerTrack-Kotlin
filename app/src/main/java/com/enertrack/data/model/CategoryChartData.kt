package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class CategoryChartData(
    @SerializedName("categoryName")
    val categoryName: String,

    // ================== PERBAIKAN DI SINI ==================
    @SerializedName("value") // Ganti @SerializedName juga jika perlu agar cocok dengan JSON dari API
    val value: Float,     // Ganti nama field dari 'usage' menjadi 'value'
    // =======================================================

    @SerializedName("color")
    val color: String
)
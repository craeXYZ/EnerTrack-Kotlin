package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class InsightResponse(
    val grade: String,      // Contoh: "A", "B", "C"
    val message: String,    // Contoh: "Excellent! Very energy efficient."
    val percentage: Int,    // Contoh: 80
    val tips: List<Tip>,
    @SerializedName("calculation_basis")
    val calculationBasis: String // Contoh: "monthly_projection"
)

data class Tip(
    val title: String,
    val description: String,
    @SerializedName("icon_type")
    val iconType: String // "ac", "plug", "lamp", "general"
)
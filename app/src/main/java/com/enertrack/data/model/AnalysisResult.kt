package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class AnalysisResult(
    @SerializedName("total_power_wh")
    val totalPowerWh: Double,

    @SerializedName("daily_kwh")
    val dailyKwh: Double,

    @SerializedName("monthly_kwh")
    val monthlyKwh: Double,

    @SerializedName("tariff_rate")
    val tariffRate: Double,

    @SerializedName("estimated_monthly_rp")
    val estimatedMonthlyRp: String,

    @SerializedName("ai_response")
    val aiResponse: String,

    @SerializedName("id_submit")
    val idSubmit: String,

    @SerializedName("besar_listrik")
    val besarListrik: String
)
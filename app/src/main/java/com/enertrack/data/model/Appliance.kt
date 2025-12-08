package com.enertrack.data.model

data class Appliance(
    val id: Long,
    val name: String,
    val brand: String?,
    val category: String?,
    val category_id: Int?,
    val powerRating: Double, // <-- PERBAIKAN
    val dailyUsage: Double,  // <-- PERBAIKAN
    val quantity: Int,
    val dailyEnergy: Double,
    val weeklyEnergy: Double,
    val monthlyEnergy: Double,
    val dailyCost: Double,
    val weeklyCost: Double,
    val monthlyCost: Double
)
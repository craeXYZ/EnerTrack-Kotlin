package com.enertrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_items")
data class HistoryItemEntity(
    @PrimaryKey
    val id: String, // ID dari server (angka string) atau lokal (UUID string)

    // Field dari API (nullable)
    val date: String?,
    val appliance: String?,
    val applianceDetails: String?, // brand
    val categoryId: Int?,
    val categoryName: String?,
    val houseCapacity: String?,
    val power: Double?,
    val usage: Double?,

    // Properti kalkulasi (non-nullable dengan default)
    val dailyKwh: Double = 0.0,
    val monthlyKwh: Double = 0.0,
    // --- PERBAIKAN DI SINI ---
    val cost: String = "", // <-- Tambahkan default value ""
    // -------------------------

    // Status sinkronisasi (non-nullable)
    val statusSync: String = "SYNCED" // Beri default SYNCED
)

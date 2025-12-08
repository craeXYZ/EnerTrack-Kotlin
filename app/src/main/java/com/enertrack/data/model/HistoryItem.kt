package com.enertrack.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class HistoryItem(
    @SerializedName("id")
    val id: String?, // <-- HARUS ADA ?

    @SerializedName("tanggal_input")
    val date: String?, // <-- HARUS ADA ?

    @SerializedName("nama_perangkat")
    val appliance: String?, // <-- HARUS ADA ?

    @SerializedName("brand")
    val applianceDetails: String?, // <-- HARUS ADA ?

    @SerializedName("category_id")
    val categoryId: Int?, // <-- HARUS ADA ?

    @SerializedName("category_name")
    val categoryName: String?, // <-- HARUS ADA ?

    @SerializedName("besar_listrik")
    val houseCapacity: String?, // <-- HARUS ADA ?

    @SerializedName("daya")
    val power: Double?, // <-- HARUS ADA ?

    @SerializedName("durasi")
    val usage: Double?, // <-- HARUS ADA ?

    // Properti ini tidak ada di JSON, jadi aman gak pakai ?
    // TAPI kalau suatu saat API-nya berubah dan ngirim null, ini juga harus ditambah ?
    val dailyKwh: Double = 0.0,
    val monthlyKwh: Double = 0.0,
    val cost: String = ""
): Serializable


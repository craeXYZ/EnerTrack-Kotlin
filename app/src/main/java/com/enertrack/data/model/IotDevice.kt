package com.enertrack.data.model

import com.google.firebase.Timestamp

data class IotDevice(
    val docId: String = "",
    val user_id: Int = 0,
    val device_name: String = "",
    val status: String = "OFFLINE",
    val watt: Double = 0.0,
    val voltase: Double = 0.0,
    val ampere: Double = 0.0,
    // FIX: Field kwh_total dihapus agar bersih
    val last_update: Timestamp? = null
) {
    // Tambahkan fungsi helper toDate (jika kamu membutuhkannya di CalculateFragment)
    fun toDate(): java.util.Date? = last_update?.toDate()
}
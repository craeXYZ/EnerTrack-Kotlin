package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

// Merepresentasikan data pengguna yang login
data class User(
    // PENTING: Ini kuncinya! Kita kasih tahu kalau JSON "user_id" itu masuk ke variabel "uid"
    @SerializedName("user_id")
    val uid: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("email")
    val email: String? = null,

    // Field tambahan buat jaga-jaga kalau server kirim
    @SerializedName("image")
    val image: String? = null,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("success")
    val success: Boolean = false
)
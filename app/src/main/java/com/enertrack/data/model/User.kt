package com.enertrack.data.model

// Merepresentasikan data pengguna yang login
data class User(
    val uid: String,
    val email: String?,
    val username: String?,
    val image: String?
)
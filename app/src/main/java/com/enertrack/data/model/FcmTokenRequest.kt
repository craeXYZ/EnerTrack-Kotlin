package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class FcmTokenRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("fcm_token") val fcmToken: String
)
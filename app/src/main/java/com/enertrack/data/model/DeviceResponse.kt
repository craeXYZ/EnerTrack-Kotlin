package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class DeviceResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("power_watt")
    val powerWatt: Double,

    @SerializedName("category_id")
    val categoryId: Int
)
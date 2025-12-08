package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class ApplianceInput(
    @SerializedName("name")
    val name: String,

    @SerializedName("details")
    val details: String,

    @SerializedName("power")
    val power: Int,

    @SerializedName("categoryId")
    val categoryId: Int
)

// Di dalam: com/enertrack/data/model/ApplianceUpdate.kt
package com.enertrack.data.model

import com.google.gson.annotations.SerializedName

data class ApplianceUpdate(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("details")
    val details: String,

    @SerializedName("power")
    val power: Int,

    @SerializedName("categoryId")
    val categoryId: Int
)

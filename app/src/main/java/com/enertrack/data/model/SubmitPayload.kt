package com.enertrack.data.model

data class SubmitPayload(
    val billingtype: String,
    val electricity: Map<String, Double?>?,
    val devices: List<DevicePayload>
)
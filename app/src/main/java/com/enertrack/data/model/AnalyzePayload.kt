package com.enertrack.data.model

data class AnalyzePayload(
    val devices: List<DevicePayload>,
    val besar_listrik: String
)
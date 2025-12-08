package com.enertrack.data.model

data class SubmitResponseData(
    val id_submit: String,
    val total_items: Int,
    val message: String?,
    val ai_response: String?
)
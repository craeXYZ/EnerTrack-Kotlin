package com.enertrack.data.model

// ==========================================
// 1. MODEL UNTUK UI (TAMPILAN)
// ==========================================
data class ChatMessage(
    val message: String,
    val isUser: Boolean, // true = Kanan (User), false = Kiri (AI)
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// 2. MODEL UNTUK API (BACKEND)
// ==========================================

// Amplop buat kirim pertanyaan ke Backend
data class ChatRequest(
    val message: String,
    val context: String
)

// Amplop buat terima jawaban dari Backend
data class ChatResponse(
    val reply: String
)

// Model buat Dropdown (Spinner)
data class DeviceOption(
    val label: String,   // Nama di layar
    val context: String  // Data rahasia buat AI
)
package com.enertrack.util

/**
 * Sealed class untuk merepresentasikan state di UI.
 * Ini akan memberitahu Fragment apakah harus menampilkan loading,
 * data (Success), atau pesan error.
 */
sealed class UIState<out T> {
    data object Loading : UIState<Nothing>()
    data class Success<out T>(val data: T) : UIState<T>()
    data class Error(val message: String) : UIState<Nothing>()
}
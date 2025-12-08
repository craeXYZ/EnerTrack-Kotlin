package com.enertrack.data.repository

/**
 * File ini berisi fungsi "pembantu" atau extension.
 * Ini yang memungkinkan kita menulis kode .onSuccess{...} dan .onFailure{...}
 * di ViewModel, membuat kodenya jadi lebih bersih.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T> Result<T>.onFailure(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Failure) {
        action(exception)
    }
    return this
}
package com.example.ui.viewmodel

sealed class ScreenState<out T> {
    object Loading : ScreenState<Nothing>()
    data class Success<T>(val data: T) : ScreenState<T>()
    data class Error(val message: String, val retry: () -> Unit = {}) : ScreenState<Nothing>()
}

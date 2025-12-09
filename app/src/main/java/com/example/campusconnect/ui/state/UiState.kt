package com.example.campusconnect.ui.state

/**
 * Sealed class representing UI state for consistent loading/error/success handling.
 * Enhanced with retry callbacks and error type classification.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()

    data class Success<T>(val data: T) : UiState<T>()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val errorType: ErrorType = ErrorType.GENERIC,
        val retry: (() -> Unit)? = null
    ) : UiState<Nothing>() {

        enum class ErrorType {
            NETWORK,        // Network connectivity issues
            AUTH,           // Authentication/authorization failures
            PERMISSION,     // Permission denied
            VALIDATION,     // Input validation errors
            NOT_FOUND,      // Resource not found
            SERVER_ERROR,   // Server-side errors
            GENERIC         // Generic/unknown errors
        }
    }

    // Helper properties
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    // Get data or null
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    // Get error or null
    fun getErrorOrNull(): Error? = when (this) {
        is Error -> this
        else -> null
    }
}


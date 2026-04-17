package com.example.campusconnect.util

import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreErrorMapper {
    fun toUserMessage(error: Throwable?, isAuthenticated: Boolean): String {
        val firestoreError = error as? FirebaseFirestoreException
        if (firestoreError != null) {
            return when (firestoreError.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    if (!isAuthenticated) {
                        "Your session has expired. Please sign in again."
                    } else {
                        "You do not have permission to access this content."
                    }
                }
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Please sign in to continue."
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Network is unavailable. Please try again."
                else -> firestoreError.message ?: "Something went wrong. Please try again."
            }
        }

        val message = error?.message.orEmpty()
        return when {
            message.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                if (!isAuthenticated) {
                    "Your session has expired. Please sign in again."
                } else {
                    "You do not have permission to access this content."
                }
            }
            message.contains("UNAUTHENTICATED", ignoreCase = true) ->
                "Please sign in to continue."
            message.isNotBlank() -> message
            else -> "Something went wrong. Please try again."
        }
    }
}


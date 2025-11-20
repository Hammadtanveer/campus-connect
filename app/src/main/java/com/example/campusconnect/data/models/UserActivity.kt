package com.example.campusconnect.data.models

data class UserActivity(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val iconResId: Int
)

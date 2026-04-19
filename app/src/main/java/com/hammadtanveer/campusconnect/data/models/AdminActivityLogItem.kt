package com.hammadtanveer.campusconnect.data.models

import com.google.firebase.Timestamp

data class AdminActivityLogItem(
    val id: String = "",
    val action: String = "",
    val userName: String = "",
    val timestamp: Timestamp? = null,
    val type: String = "",
    val userId: String = ""
)

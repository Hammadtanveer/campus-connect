package com.hammadtanveer.campusconnect.data.models

import com.google.firebase.Timestamp

data class SeniorProfile(
    val id: String = "",
    val name: String = "",
    val branch: String = "",
    val batch: String = "",
    val companyPlaced: String = "",
    val linkedinUrl: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val createdAt: Timestamp? = null
)

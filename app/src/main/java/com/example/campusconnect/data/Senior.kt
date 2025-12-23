package com.example.campusconnect.data

data class Senior(
    val id: String = "",
    val name: String = "",
    val branch: String = "",
    val year: String = "",
    val mobileNumber: String = "",
    val photoUrl: String = "", // URL from Cloudinary
    val linkedinUrl: String = "",
    val bio: String = ""
)


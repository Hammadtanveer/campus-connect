package com.example.campusconnect.util

object Constants {
    // Admin code for granting admin privileges during registration or upgrade
    const val ADMIN_CODE = "CAMPUS_ADMIN_2025"
    // Default admin roles granted on upgrade (mirrors server-side expectations)
    val DEFAULT_ADMIN_ROLES = listOf("admin", "event:create", "notes:upload")

    // File size limits
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
    const val MAX_FILE_SIZE_MB = 10

    // Allowed file types - PDF ONLY FOR NOTES
    val ALLOWED_FILE_TYPES = setOf(
        "pdf", "PDF"
    )

    // MIME types - PDF ONLY
    val ALLOWED_MIME_TYPES = setOf(
        "application/pdf"
    )

    // Cloudinary folder structure
    const val CLOUDINARY_BASE_FOLDER = "campus_notes"

    // Subjects
    val SUBJECTS = listOf(
        "Mathematics",
        "Physics",
        "Chemistry",
        "Biology",
        "Computer Science",
        "Electronics",
        "Mechanical Engineering",
        "Civil Engineering",
        "Electrical Engineering",
        "English",
        "Business Studies",
        "Economics",
        "Other"
    )

    // Semesters
    val SEMESTERS = listOf(
        "Semester 1",
        "Semester 2",
        "Semester 3",
        "Semester 4",
        "Semester 5",
        "Semester 6",
        "Semester 7",
        "Semester 8"
    )
}

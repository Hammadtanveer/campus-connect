package com.example.campusconnect.util

import com.google.firebase.Timestamp

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

    // Subjects (legacy - kept for backward compatibility)
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

    // Subject code data class
    data class SubjectCode(
        val code: String,
        val name: String,
        val semester: String
    ) {
        val displayText: String get() = "$code - $name"
    }

    // Comprehensive subject codes mapped to semesters
    val SUBJECT_CODES_BY_SEMESTER = mapOf(
        "Semester 1" to listOf(
            SubjectCode("CSE101", "Programming Fundamentals", "Semester 1"),
            SubjectCode("MTH101", "Engineering Mathematics I", "Semester 1"),
            SubjectCode("PHY101", "Engineering Physics", "Semester 1"),
            SubjectCode("CHM101", "Engineering Chemistry", "Semester 1"),
            SubjectCode("ENG101", "Communication Skills", "Semester 1"),
            SubjectCode("MEE101", "Engineering Graphics", "Semester 1")
        ),
        "Semester 2" to listOf(
            SubjectCode("CSE102", "Data Structures", "Semester 2"),
            SubjectCode("MTH102", "Engineering Mathematics II", "Semester 2"),
            SubjectCode("PHY102", "Applied Physics", "Semester 2"),
            SubjectCode("EEE102", "Basic Electronics", "Semester 2"),
            SubjectCode("CSE103", "Digital Logic Design", "Semester 2"),
            SubjectCode("ENV102", "Environmental Studies", "Semester 2")
        ),
        "Semester 3" to listOf(
            SubjectCode("CSE201", "Object Oriented Programming", "Semester 3"),
            SubjectCode("CSE202", "Database Management Systems", "Semester 3"),
            SubjectCode("CSE203", "Computer Organization", "Semester 3"),
            SubjectCode("MTH201", "Discrete Mathematics", "Semester 3"),
            SubjectCode("CSE204", "Operating Systems", "Semester 3"),
            SubjectCode("HUM201", "Professional Ethics", "Semester 3")
        ),
        "Semester 4" to listOf(
            SubjectCode("CSE301", "Algorithms", "Semester 4"),
            SubjectCode("CSE302", "Software Engineering", "Semester 4"),
            SubjectCode("CSE303", "Computer Networks", "Semester 4"),
            SubjectCode("CSE304", "Theory of Computation", "Semester 4"),
            SubjectCode("CSE305", "Microprocessors", "Semester 4"),
            SubjectCode("MGT301", "Organizational Behavior", "Semester 4")
        ),
        "Semester 5" to listOf(
            SubjectCode("CSE401", "Web Technologies", "Semester 5"),
            SubjectCode("CSE402", "Machine Learning", "Semester 5"),
            SubjectCode("CSE403", "Compiler Design", "Semester 5"),
            SubjectCode("CSE404", "Cloud Computing", "Semester 5"),
            SubjectCode("CSE405", "Information Security", "Semester 5"),
            SubjectCode("CSE4E1", "Elective I", "Semester 5")
        ),
        "Semester 6" to listOf(
            SubjectCode("CSE501", "Artificial Intelligence", "Semester 6"),
            SubjectCode("CSE502", "Mobile Application Development", "Semester 6"),
            SubjectCode("CSE503", "Big Data Analytics", "Semester 6"),
            SubjectCode("CSE504", "Internet of Things", "Semester 6"),
            SubjectCode("CSE505", "Blockchain Technology", "Semester 6"),
            SubjectCode("CSE5E2", "Elective II", "Semester 6")
        ),
        "Semester 7" to listOf(
            SubjectCode("CSE601", "Distributed Systems", "Semester 7"),
            SubjectCode("CSE602", "Advanced Database Systems", "Semester 7"),
            SubjectCode("CSE603", "Natural Language Processing", "Semester 7"),
            SubjectCode("CSE604", "Computer Vision", "Semester 7"),
            SubjectCode("CSE6E3", "Elective III", "Semester 7"),
            SubjectCode("CSE690", "Major Project I", "Semester 7")
        ),
        "Semester 8" to listOf(
            SubjectCode("CSE701", "Deep Learning", "Semester 8"),
            SubjectCode("CSE702", "DevOps and Automation", "Semester 8"),
            SubjectCode("CSE703", "Quantum Computing", "Semester 8"),
            SubjectCode("CSE7E4", "Elective IV", "Semester 8"),
            SubjectCode("CSE790", "Major Project II", "Semester 8"),
            SubjectCode("INT701", "Industry Internship", "Semester 8")
        )
    )

    // Get all subject codes for a specific semester
    fun getSubjectCodesForSemester(semester: String): List<SubjectCode> {
        return SUBJECT_CODES_BY_SEMESTER[semester] ?: emptyList()
    }

    // Get all unique subject codes
    fun getAllSubjectCodes(): List<SubjectCode> {
        return SUBJECT_CODES_BY_SEMESTER.values.flatten()
    }

    // Find subject code by code string
    fun findSubjectByCode(code: String): SubjectCode? {
        return getAllSubjectCodes().find { it.code == code }
    }
}

// Utility function to get current Firestore Timestamp
fun getCurrentTimestamp(): Timestamp = Timestamp.now()

// Utility function to format Timestamp as String
fun formatTimestamp(timestamp: Timestamp = Timestamp.now()): String {
    val date = timestamp.toDate()
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(date)
}

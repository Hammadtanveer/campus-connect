package com.example.campusconnect.data.repository

import androidx.compose.runtime.mutableStateOf
import com.example.campusconnect.data.models.UserActivity
import com.example.campusconnect.data.models.ActivityType
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogRepository @Inject constructor() {
    private val _activities = mutableStateOf<List<UserActivity>>(emptyList())
    val activities = _activities

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun logActivity(type: ActivityType, description: String) {
        val newActivity = UserActivity(
            id = UUID.randomUUID().toString(),
            type = type.name,
            title = type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            description = description,
            timestamp = dateFormat.format(Date()),
            iconResId = getIconForType(type)
        )
        _activities.value = listOf(newActivity) + _activities.value.take(99) // Keep last 100
    }

    fun clearActivities() {
        _activities.value = emptyList()
    }

    private fun getIconForType(type: ActivityType): Int {
        // Return default icon resource id - can be customized per type
        return android.R.drawable.ic_dialog_info
    }
}


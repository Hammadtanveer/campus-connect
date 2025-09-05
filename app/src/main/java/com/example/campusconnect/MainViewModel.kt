package com.example.campusconnect

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Use State for Compose compatibility
    private val _initializing = mutableStateOf(true)
    val initializing: Boolean get() = _initializing.value

    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: UserProfile? get() = _userProfile.value

    private val _currentScreen = mutableStateOf<Screen>(Screen.DrawerScreen.Profile)
    val currentScreen: Screen get() = _currentScreen.value

    private val _userActivities = mutableStateOf<List<UserActivity>>(emptyList())
    val userActivities: List<UserActivity> get() = _userActivities.value

    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val sizeLabel: String
    )
    private val _downloads = mutableStateOf<List<DownloadItem>>(emptyList())
    val downloads: List<DownloadItem> get() = _downloads.value

    init {
        auth.currentUser?.uid?.let { loadUserProfile(it) } ?: run { _initializing.value = false }
    }

    // All your existing methods with State updates
    private fun loadUserProfile(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _userProfile.value = doc.toObject(UserProfile::class.java)
                    loadUserActivities(userId)
                }
                _initializing.value = false
            }
            .addOnFailureListener {
                _initializing.value = false
            }
    }

    fun signInWithEmailPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let {
                        loadUserProfile(it)
                        onResult(true, null)
                    } ?: onResult(false, "No user id")
                } else {
                    onResult(false, task.exception?.message ?: "Authentication failed")
                }
            }
    }

    fun registerWithEmailPassword(
        email: String,
        password: String,
        displayName: String,
        course: String,
        branch: String,
        year: String,
        bio: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let { uid ->
                        createUserProfile(
                            userId = uid,
                            displayName = displayName,
                            email = email,
                            course = course,
                            branch = branch,
                            year = year,
                            bio = bio
                        )
                        onResult(true, null)
                    } ?: onResult(false, "No user id")
                } else {
                    onResult(false, task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun createUserProfile(
        userId: String,
        displayName: String,
        email: String,
        course: String,
        branch: String,
        year: String,
        bio: String = ""
    ) {
        val db = FirebaseFirestore.getInstance()
        val profile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            course = course,
            branch = branch,
            year = year,
            bio = bio
        )
        db.collection("users").document(userId)
            .set(profile)
            .addOnSuccessListener {
                _userProfile.value = profile
                _initializing.value = false
                _currentScreen.value = Screen.DrawerScreen.Profile
            }
            .addOnFailureListener {
                _initializing.value = false
            }
    }

    fun signOut() {
        auth.signOut()
        _userProfile.value = null
        _currentScreen.value = Screen.DrawerScreen.Profile
    }

    fun setCurrentScreenByRoute(route: String) {
        val newScreen: Screen.DrawerScreen = when (route) {
            Screen.DrawerScreen.Profile.dRoute -> Screen.DrawerScreen.Profile
            Screen.DrawerScreen.Download.dRoute -> Screen.DrawerScreen.Download
            Screen.DrawerScreen.Notes.dRoute -> Screen.DrawerScreen.Notes
            Screen.DrawerScreen.Seniors.dRoute -> Screen.DrawerScreen.Seniors
            Screen.DrawerScreen.Societies.dRoute -> Screen.DrawerScreen.Societies
            Screen.DrawerScreen.PlacementCareer.dRoute -> Screen.DrawerScreen.PlacementCareer
            Screen.DrawerScreen.OnlineMeetingsEvents.dRoute -> Screen.DrawerScreen.OnlineMeetingsEvents
            else -> return
        }
        if (_currentScreen.value.route != newScreen.dRoute) {
            _currentScreen.value = newScreen
        }
    }

    fun addDownload(title: String, sizeLabel: String) {
        _downloads.value = _downloads.value + DownloadItem(title = title, sizeLabel = sizeLabel)
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filterNot { it.id == id }
    }

    fun clearDownloads() {
        _downloads.value = emptyList()
    }

    fun updateUserProfile(updatedProfile: UserProfile, onResult: (Boolean, String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(updatedProfile.id)
            .set(updatedProfile)
            .addOnSuccessListener {
                _userProfile.value = updatedProfile
                addActivity(
                    UserActivity(
                        id = UUID.randomUUID().toString(),
                        type = ActivityType.PROFILE_UPDATE.name,
                        title = "Profile Updated",
                        description = "You updated your profile information",
                        timestamp = getCurrentTimestamp(),
                        iconResId = R.drawable.outline_person_24
                    )
                )
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    fun trackNoteUpload(noteTitle: String) {
        addActivity(
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.NOTE_UPLOAD.name,
                title = "Note Uploaded",
                description = "You uploaded: $noteTitle",
                timestamp = getCurrentTimestamp(),
                iconResId = R.drawable.baseline_notes_24
            )
        )
    }

    fun trackEventJoin(eventName: String) {
        addActivity(
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.EVENT_JOINED.name,
                title = "Event Joined",
                description = "You joined: $eventName",
                timestamp = getCurrentTimestamp(),
                iconResId = R.drawable.baseline_event_24
            )
        )
    }

    fun trackNoteDownload(noteTitle: String) {
        addActivity(
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.NOTE_DOWNLOAD.name,
                title = "Note Downloaded",
                description = "You downloaded: $noteTitle",
                timestamp = getCurrentTimestamp(),
                iconResId = R.drawable.outline_download_24
            )
        )
    }

    private fun addActivity(activity: UserActivity) {
        _userActivities.value = _userActivities.value + activity
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    fun loadUserActivities(userId: String) {
        val sampleActivities = listOf(
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.NOTE_UPLOAD.name,
                title = "Note Uploaded",
                description = "You uploaded: Data Structures Lecture Notes",
                timestamp = "Oct 15, 2023 14:30",
                iconResId = R.drawable.baseline_notes_24
            ),
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.EVENT_JOINED.name,
                title = "Event Joined",
                description = "You joined: Tech Symposium 2023",
                timestamp = "Oct 10, 2023 10:15",
                iconResId = R.drawable.baseline_event_24
            ),
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.NOTE_DOWNLOAD.name,
                title = "Note Downloaded",
                description = "You downloaded: Algorithms Cheat Sheet",
                timestamp = "Oct 05, 2023 16:45",
                iconResId = R.drawable.outline_download_24
            )
        )
        _userActivities.value = sampleActivities
    }
}
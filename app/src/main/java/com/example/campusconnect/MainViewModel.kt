package com.example.campusconnect

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

    // Events-related state
    private val eventsRepo = EventsRepository(FirebaseFirestore.getInstance())

    private val _eventsList = mutableStateOf<List<OnlineEvent>>(emptyList())
    val eventsList: List<OnlineEvent> get() = _eventsList.value

    private val _currentEvent = mutableStateOf<OnlineEvent?>(null)
    val currentEvent: OnlineEvent? get() = _currentEvent.value

    private val _isLoadingEvents = mutableStateOf(false)
    val isLoadingEvents: Boolean get() = _isLoadingEvents.value

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
                    // Navigate into the app after successful profile load
                    _currentScreen.value = Screen.DrawerScreen.Profile
                    Log.i("MainViewModel", "loadUserProfile: profile loaded, navigating to Profile for $userId")
                } else {
                    // Firestore document missing — attempt to build a fallback profile from FirebaseAuth user
                    Log.w("MainViewModel", "loadUserProfile: no document found for $userId, creating fallback profile from auth user")
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val fallback = UserProfile(
                            id = firebaseUser.uid,
                            displayName = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            course = "",
                            branch = "",
                            year = "",
                            bio = "",
                        )
                        // set locally so AuthGate treats the user as authenticated
                        _userProfile.value = fallback
                        _currentScreen.value = Screen.DrawerScreen.Profile
                        // write the fallback profile to Firestore (best-effort)
                        db.collection("users").document(firebaseUser.uid)
                            .set(fallback)
                            .addOnSuccessListener {
                                Log.i("MainViewModel", "loadUserProfile: wrote fallback profile for ${firebaseUser.uid}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainViewModel", "loadUserProfile: failed to write fallback profile", e)
                            }
                    }
                }
                _initializing.value = false
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "loadUserProfile failed", e)
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
                    val uid = auth.currentUser?.uid
                    Log.i("MainViewModel", "signInWithEmailPassword: success, uid=$uid")
                    if (uid != null) {
                        // Immediately set a minimal local profile so UI considers user authenticated
                        val firebaseUser = auth.currentUser
                        if (firebaseUser != null) {
                            _userProfile.value = UserProfile(
                                id = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: "",
                                email = firebaseUser.email ?: "",
                                course = "",
                                branch = "",
                                year = "",
                                bio = ""
                            )
                            // set current screen so MainView becomes visible
                            _currentScreen.value = Screen.DrawerScreen.Profile
                        }

                        // Then attempt to load the full profile from Firestore (best-effort)
                        loadUserProfile(uid)
                        onResult(true, null)
                    } else {
                        onResult(false, "Signed in but no user id. Please restart the app.")
                    }
                } else {
                    val ex = task.exception
                    Log.e("MainViewModel", "signIn failed", ex)
                    val lower = ex?.localizedMessage?.lowercase() ?: ""
                    val message = when (ex) {
                        is FirebaseAuthException -> {
                            val code = try { ex.errorCode } catch (_: Exception) { null }
                            when (code) {
                                "ERROR_USER_NOT_FOUND", "USER_NOT_FOUND" -> "No user found with this email. Please sign up first."
                                "ERROR_WRONG_PASSWORD", "WRONG_PASSWORD" -> "Incorrect password. Please try again."
                                "ERROR_USER_DISABLED" -> "This user account has been disabled."
                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your internet connection and try again."
                                else -> ex.localizedMessage ?: "Authentication failed."
                            }
                        }
                        else -> when {
                            "network" in lower || "timeout" in lower || "unreachable" in lower ->
                                "Network error. Please check your connection and try again."
                            else -> ex?.localizedMessage ?: "Authentication failed."
                        }
                    }
                    onResult(false, message)
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
        Log.i("MainViewModel", "registerWithEmailPassword: starting for $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("MainViewModel", "createUserWithEmailAndPassword succeeded")
                    val user = auth.currentUser
                    if (user == null) {
                        onResult(false, "Registration succeeded but no user instance found.")
                        return@addOnCompleteListener
                    }

                    // Update display name in FirebaseAuth profile
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { updateTask ->
                            if (!updateTask.isSuccessful) {
                                Log.w("MainViewModel", "updateProfile failed", updateTask.exception)
                                // Continue even if updating displayName failed
                            }

                            // Create Firestore profile and call onResult only after write completes
                            createUserProfile(
                                userId = user.uid,
                                displayName = displayName,
                                email = email,
                                course = course,
                                branch = branch,
                                year = year,
                                bio = bio
                            ) { ok, err ->
                                if (ok) {
                                    Log.i("MainViewModel", "createUserProfile succeeded for ${user.uid}")
                                    onResult(true, null)
                                } else {
                                    Log.e("MainViewModel", "createUserProfile failed: $err")
                                    onResult(false, err ?: "Failed to save user profile.")
                                }
                            }
                        }
                } else {
                    val ex = task.exception
                    Log.e("MainViewModel", "registerWithEmailPassword failed", ex)
                    val lower = ex?.localizedMessage?.lowercase() ?: ""
                    val message = when (ex) {
                        is FirebaseAuthException -> {
                            val code = try { ex.errorCode } catch (_: Exception) { null }
                            when (code) {
                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your internet connection and try again."
                                "ERROR_WEAK_PASSWORD", "WEAK_PASSWORD" -> "Password is too weak. Please use at least 6 characters."
                                "ERROR_EMAIL_ALREADY_IN_USE", "EMAIL_EXISTS" -> "This email is already in use. Try signing in or use a different email."
                                "ERROR_INVALID_EMAIL", "INVALID_EMAIL" -> "The email address is invalid. Check for typos."
                                else -> ex.localizedMessage ?: "Registration failed."
                            }
                        }
                        else -> when {
                            "network" in lower || "timeout" in lower || "unreachable" in lower ->
                                "Network error. Please check your connection and try again."
                            else -> ex?.localizedMessage ?: "Registration failed."
                        }
                    }
                    onResult(false, message)
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
        bio: String = "",
        onResult: (Boolean, String?) -> Unit
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
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "createUserProfile failed", e)
                _initializing.value = false
                onResult(false, e.message)
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
            Screen.DrawerScreen.Events.dRoute -> Screen.DrawerScreen.Events
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

    // Helper to call trackEventJoin by event id (will fetch event title if available)
    fun trackEventJoinById(eventId: String) {
        // Try to find in local list first
        val event = _eventsList.value.find { it.id == eventId } ?: _currentEvent.value
        if (event != null) {
            trackEventJoin(event.title)
            return
        }
        // Fallback: read from Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val e = doc.toObject(OnlineEvent::class.java)
                    if (e != null) trackEventJoin(e.title)
                }
            }
            .addOnFailureListener { }
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

    // Events-related helper methods that call into repository
    fun loadEvents(): Flow<Resource<List<OnlineEvent>>> {
        return eventsRepo.observeEvents()
    }

    fun createEvent(
        title: String,
        description: String,
        dateTime: com.google.firebase.Timestamp,
        durationMinutes: Long,
        category: EventCategory,
        maxParticipants: Int = 0,
        meetLink: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val organizerId = auth.currentUser?.uid ?: return onResult(false, "Not authenticated")
        val organizerName = auth.currentUser?.displayName ?: ""
        eventsRepo.createEvent(
            title = title,
            description = description,
            dateTime = dateTime,
            durationMinutes = durationMinutes,
            organizerId = organizerId,
            organizerName = organizerName,
            category = category,
            maxParticipants = maxParticipants,
            meetLink = meetLink
        ) { ok, err ->
            onResult(ok, err)
        }
    }

    fun registerForEvent(eventId: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) return onResult(false, "Not authenticated")
        eventsRepo.registerForEvent(userId = userId, eventId = eventId) { ok, err ->
            if (ok) {
                // Track activity (find event title if possible)
                val eventTitle = _eventsList.value.find { it.id == eventId }?.title
                if (eventTitle != null) trackEventJoin(eventTitle) else trackEventJoinById(eventId)
            }
            onResult(ok, err)
        }
    }

    fun getMyEventRegistrations(): Flow<Resource<List<EventRegistration>>> {
        val userId = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(Resource.Error("Not authenticated"))
        return eventsRepo.observeMyRegistrations(userId)
    }

    // Start background observation to populate local events list
    private fun refreshEvents() {
        viewModelScope.launch {
            eventsRepo.observeEvents().collect { res ->
                when (res) {
                    is Resource.Loading -> _isLoadingEvents.value = true
                    is Resource.Success -> {
                        _isLoadingEvents.value = false
                        _eventsList.value = res.data
                    }
                    is Resource.Error -> {
                        _isLoadingEvents.value = false
                        // Keep existing list if error; could log
                        Log.w("MainViewModel", "refreshEvents error: ${res.message}")
                    }
                }
            }
        }
    }

    init {
        // ensure events are observed into local state
        refreshEvents()
    }
}

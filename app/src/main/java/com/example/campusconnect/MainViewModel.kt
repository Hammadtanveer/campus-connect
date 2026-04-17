package com.example.campusconnect

import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateOf
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.models.UserActivity
import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.NotesRepository
import com.example.campusconnect.data.repository.SeniorsRepository
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import com.example.campusconnect.security.canUploadNotes
import com.example.campusconnect.security.canUpdateSenior
import com.example.campusconnect.security.canManageSociety
import com.example.campusconnect.security.canAddSenior
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Note
import kotlinx.coroutines.launch
import android.app.Application
import com.example.campusconnect.util.formatTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.data.repository.AdminActivityLogRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import com.example.campusconnect.data.Senior
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import com.example.campusconnect.notifications.NotificationSubscriptionManager
import com.example.campusconnect.security.PermissionManager
import com.example.campusconnect.util.DbgLog
import com.example.campusconnect.util.UserProfileMapper

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val auth: FirebaseAuth,
    // private val eventsRepo: EventsRepository, // Removed unused
    private val notesRepo: NotesRepository,
    private val seniorsRepo: SeniorsRepository,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val activityLogRepository: ActivityLogRepository,
    private val adminActivityLogRepository: AdminActivityLogRepository
) : AndroidViewModel(application) {
    companion object {
        private const val SENIOR_TAG = "VM"
    }

    // Use State for Compose compatibility
    private val _initializing = mutableStateOf(true)
    val initializing: Boolean get() = _initializing.value

    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: UserProfile? get() = _userProfile.value
    val sessionProfileFlow: Flow<UserProfile?> = sessionManager.state.map { it.profile }

    private val _currentScreen = mutableStateOf<Screen>(Screen.DrawerScreen.Profile)
    val currentScreen: Screen get() = _currentScreen.value

    val userActivities: List<UserActivity> get() = activityLogRepository.activities.value

    // notifications/unread badge
    private val _unreadEventNotifications = mutableStateOf(0)
    val unreadEventNotifications: Int get() = _unreadEventNotifications.value

    // pending mentorship requests badge (Removed feature)
    // private val _pendingMentorshipRequests = mutableStateOf(0)
    // val pendingMentorshipRequests: Int get() = _pendingMentorshipRequests.value

    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val sizeLabel: String
    )
    private val _downloads = mutableStateOf<List<DownloadItem>>(emptyList())
    val downloads: List<DownloadItem> get() = _downloads.value

    // Events-related state moved to EventsViewModel
    /*
    private val _eventsList = mutableStateOf<List<OnlineEvent>>(emptyList())
    @Suppress("unused")
    val eventsList: List<OnlineEvent> get() = _eventsList.value

    private val _currentEvent = mutableStateOf<OnlineEvent?>(null)
    @Suppress("unused")
    val currentEvent: OnlineEvent? get() = _currentEvent.value

    private val _isLoadingEvents = mutableStateOf(false)
    @Suppress("unused")
    val isLoadingEvents: Boolean get() = _isLoadingEvents.value
    */

    private val _myNotes = mutableStateOf<List<Note>>(emptyList())
    val myNotes: List<Note> get() = _myNotes.value

    // Seniors
    private val _seniorsList = MutableStateFlow<List<Senior>>(emptyList())
    val seniorsList: StateFlow<List<Senior>> = _seniorsList.asStateFlow()
    private var seniorsObserverJob: Job? = null
    private var userProfileListener: ListenerRegistration? = null
    private var observedProfileUid: String? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var lastObservedUid: String? = null
    private val _deleteSeniorStatus = mutableStateOf<Resource<Unit>?>(null)
    val deleteSeniorStatus: Resource<Unit>? get() = _deleteSeniorStatus.value

    // Prevent multiple subscriptions
    private var hasSubscribedTopicsThisSession = false

    private fun subscribeTopicsOncePerSession() {
        if (hasSubscribedTopicsThisSession) return
        NotificationSubscriptionManager.subscribeAllTopics()
        hasSubscribedTopicsThisSession = true
    }

    init {
        DbgLog.d(SENIOR_TAG, "init start")
        // Sync with SessionManager to keep profile updated across ViewModels
        viewModelScope.launch {
            DbgLog.d(SENIOR_TAG, "sessionManager collect start")
            sessionManager.state.collect { state ->
                DbgLog.d(SENIOR_TAG, "session update profilePresent=${state.profile != null}")
                _userProfile.value = state.profile
                val profile = state.profile
                val perms = PermissionManager.effectivePermissions(profile).sorted()
                Log.d("PERM_DEBUG", "MainViewModel session sync -> userId=${profile?.id ?: "null"}, role=${profile?.role ?: ""}, permissions=$perms")
            }
        }

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            handleAuthStateChanged(firebaseAuth.currentUser)
        }
        authStateListener?.let { auth.addAuthStateListener(it) }

        // Handle current auth synchronously so first launch doesn't depend on listener timing.
        handleAuthStateChanged(auth.currentUser)

        // Safety net: avoid indefinite splash if an async callback stalls.
        viewModelScope.launch {
            delay(6000)
            if (_initializing.value) {
                DbgLog.e(SENIOR_TAG, "initialization watchdog tripped; forcing initializing=false")
                _initializing.value = false
            }
        }
        DbgLog.d(SENIOR_TAG, "init end")
    }

    private fun handleAuthStateChanged(user: com.google.firebase.auth.FirebaseUser?) {
        val uid = user?.uid
        DbgLog.d(SENIOR_TAG, "authState changed uid=${uid ?: "null"}")

        if (uid == lastObservedUid) {
            DbgLog.d(SENIOR_TAG, "authState unchanged, skip re-init")
            return
        }

        lastObservedUid = uid
        if (user == null) {
            DbgLog.d(SENIOR_TAG, "auth user missing, clear seniors and stop initializing")
            seniorsObserverJob?.cancel()
            userProfileListener?.remove()
            userProfileListener = null
            observedProfileUid = null
            _seniorsList.value = emptyList()
            sessionManager.updateAuth(null, null)
            sessionManager.updateProfile(null)
            _initializing.value = false
            return
        }

        DbgLog.d(SENIOR_TAG, "auth user ready uid=${user.uid}, bootstrap profile + seniors")
        sessionManager.updateAuth(user.uid, user.email)
        observeUserProfile(user.uid)
        observeSeniors()
    }

    private fun observeSeniors() {
        seniorsObserverJob?.cancel()
        DbgLog.d(SENIOR_TAG, "observeSeniors collector created")
        seniorsObserverJob = viewModelScope.launch {
            seniorsRepo.observeSeniors().collect { res ->
                when (res) {
                    is Resource.Success -> {
                        DbgLog.d(SENIOR_TAG, "observeSeniors success count=${res.data.size}")
                        if (res.data != _seniorsList.value) {
                            _seniorsList.value = res.data
                            DbgLog.d(SENIOR_TAG, "seniorsList updated count=${_seniorsList.value.size}")
                        }
                    }
                    is Resource.Error -> DbgLog.e(SENIOR_TAG, "observeSeniors error=${res.message}")
                    is Resource.Loading -> DbgLog.d(SENIOR_TAG, "observeSeniors loading")
                }
            }
        }
    }

    // All your existing methods with State updates
    private fun normalizeRbac(profile: UserProfile): UserProfile {
        val normalizedRole = profile.role.trim().lowercase()
        val normalizedPermissions = profile.permissions
            .map { PermissionManager.normalizePermission(it) }
            .filter { it.isNotBlank() }
            .distinct()
        return profile.copy(
            role = normalizedRole,
            permissions = normalizedPermissions
        )
    }

    private fun observeUserProfile(userId: String) {
        if (observedProfileUid == userId && userProfileListener != null) {
            DbgLog.d(SENIOR_TAG, "observeUserProfile already active for userId=$userId")
            return
        }

        userProfileListener?.remove()
        userProfileListener = null
        observedProfileUid = userId

        DbgLog.d(SENIOR_TAG, "observeUserProfile start userId=$userId")
        userProfileListener = firestore.collection("users").document(userId)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    _initializing.value = false
                    DbgLog.e(SENIOR_TAG, "observeUserProfile error; initializing=false", error)
                    return@addSnapshotListener
                }

                if (doc == null) {
                    _initializing.value = false
                    DbgLog.e(SENIOR_TAG, "observeUserProfile null snapshot; initializing=false")
                    return@addSnapshotListener
                }

                DbgLog.d(SENIOR_TAG, "observeUserProfile snapshot exists=${doc.exists()} pendingWrites=${doc.metadata.hasPendingWrites()} fromCache=${doc.metadata.isFromCache}")

                if (doc.exists()) {
                    val profile = UserProfileMapper.fromDocument(doc)
                    val normalized = profile?.let { normalizeRbac(it) }
                    _userProfile.value = normalized
                    sessionManager.updateProfile(normalized)
                    PermissionManager.logProfileSnapshot(normalized, "MainViewModel.observeUserProfile")

                    val role = normalized?.role.orEmpty()
                    val perms = normalized?.let { PermissionManager.effectivePermissions(it).sorted() } ?: emptyList()
                    Log.d("PERM_DEBUG", "REALTIME UPDATE -> role=$role, permissions=$perms")
                    if (normalized != null && perms.isEmpty()) {
                        Log.d("PERM_DEBUG", "userId=${normalized.id}, role=${normalized.role}, permissions missing or empty")
                    }

                    loadUserActivities()
                    _currentScreen.value = Screen.DrawerScreen.Profile
                } else {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        DbgLog.d(SENIOR_TAG, "observeUserProfile fallback profile for uid=${firebaseUser.uid}")
                        val fallback = UserProfile(
                            id = firebaseUser.uid,
                            displayName = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            course = "",
                            branch = "",
                            year = "",
                            bio = "",
                        )
                        _userProfile.value = fallback
                        sessionManager.updateProfile(fallback)
                        _currentScreen.value = Screen.DrawerScreen.Profile
                        firestore.collection("users").document(firebaseUser.uid)
                            .set(fallback)
                            .addOnSuccessListener {
                                DbgLog.d(SENIOR_TAG, "observeUserProfile fallback write success uid=${firebaseUser.uid}")
                            }
                            .addOnFailureListener { writeError ->
                                DbgLog.e(SENIOR_TAG, "observeUserProfile fallback write failed", writeError)
                            }
                    }
                }

                _initializing.value = false
                DbgLog.d(SENIOR_TAG, "observeUserProfile snapshot processed; initializing=false")
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
                        observeUserProfile(uid)
                        subscribeTopicsOncePerSession()
                        onResult(true, null)
                    } else {
                        onResult(false, "Signed in but no user id. Please restart the app.")
                    }
                } else {
                    val ex = task.exception
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
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { updateTask ->
                            if (!updateTask.isSuccessful) Unit
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
                                    onResult(false, err ?: "Failed to save user profile.")
                                }
                            }
                        }
                } else {
                    val ex = task.exception
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
                            // Only map to network error if it clearly looks like a connectivity problem
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
        val profile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            course = course,
            branch = branch,
            year = year,
            bio = bio,
            permissions = emptyList()
        )
        firestore.collection("users").document(userId)
            .set(profile)
            .addOnSuccessListener {
                adminActivityLogRepository.logActionAsync(
                    action = "User registered: $displayName",
                    userName = displayName,
                    type = "user_registered",
                    userId = userId
                )
                // Update local state and session so UI recognizes admin immediately
                _userProfile.value = profile
                try {
                    sessionManager.updateAuth(profile.id, profile.email)
                    sessionManager.updateProfile(profile)
                } catch (ex: Exception) {
                    Unit
                }
                _initializing.value = false
                _currentScreen.value = Screen.DrawerScreen.Profile
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                _initializing.value = false
                onResult(false, e.message)
            }
    }

    fun signOut() {
        auth.signOut()
        _userProfile.value = null
        sessionManager.updateAuth(null, null)
        sessionManager.updateProfile(null)
        _currentScreen.value = Screen.DrawerScreen.Profile
        // cleanup listeners
        // stopPendingRequestsListener() - Mentorship removed
    }

    fun setCurrentScreenByRoute(route: String) {
        val newScreen: Screen.DrawerScreen = when (route) {
            Screen.DrawerScreen.Profile.dRoute -> Screen.DrawerScreen.Profile
            Screen.DrawerScreen.Download.dRoute -> Screen.DrawerScreen.Download
            Screen.DrawerScreen.Notes.dRoute -> Screen.DrawerScreen.Notes
            Screen.DrawerScreen.Seniors.dRoute -> Screen.DrawerScreen.Seniors
            Screen.DrawerScreen.Societies.dRoute -> Screen.DrawerScreen.Societies
            Screen.DrawerScreen.PlacementCareer.dRoute -> Screen.DrawerScreen.PlacementCareer
            Screen.DrawerScreen.Events.dRoute -> Screen.DrawerScreen.Events
            else -> return
        }
        if (_currentScreen.value.route != newScreen.dRoute) {
            _currentScreen.value = newScreen
        }
    }

    @Suppress("unused")
    fun addDownload(title: String, sizeLabel: String) {
        _downloads.value = _downloads.value + DownloadItem(title = title, sizeLabel = sizeLabel)
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filterNot { it.id == id }
    }

    fun clearDownloads() {
        _downloads.value = emptyList()
    }

    @Suppress("unused")
    fun updateUserProfile(updatedProfile: UserProfile, onResult: (Boolean, String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val safeUpdates = mapOf(
            "displayName" to updatedProfile.displayName,
            "email" to updatedProfile.email,
            "course" to updatedProfile.course,
            "branch" to updatedProfile.branch,
            "year" to updatedProfile.year,
            "bio" to updatedProfile.bio,
            "profilePictureUrl" to updatedProfile.profilePictureUrl
        )

        db.collection("users").document(updatedProfile.id)
            .set(safeUpdates, SetOptions.merge())
            .addOnSuccessListener {
                val current = _userProfile.value
                val mergedProfile = (current ?: updatedProfile).copy(
                    displayName = updatedProfile.displayName,
                    email = updatedProfile.email,
                    course = updatedProfile.course,
                    branch = updatedProfile.branch,
                    year = updatedProfile.year,
                    bio = updatedProfile.bio,
                    profilePictureUrl = updatedProfile.profilePictureUrl
                )
                _userProfile.value = mergedProfile
                sessionManager.updateProfile(mergedProfile)
                addActivity(
                    UserActivity(
                        id = UUID.randomUUID().toString(),
                        type = ActivityType.PROFILE_UPDATE.name,
                        title = "Profile Updated",
                        description = "You updated your profile information",
                        timestamp = formatTimestamp(),
                        iconResId = R.drawable.outline_person_24
                    )
                )
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }


    private fun addActivity(@Suppress("UNUSED_PARAMETER") activity: UserActivity) {
        // ActivityLogRepository uses logActivity internally
        // Activities are already being logged where needed
        Unit
    }

    fun loadUserActivities() {
        // Sample activities for demonstration
        // In production, activities come from ActivityLogRepository
    }

    // Events-related logic moved to EventsViewModel
    /*
    fun loadEvents(): Flow<Resource<List<OnlineEvent>>> {
        return eventsRepo.observeEvents()
    }

    fun createEvent(...) { ... }

    suspend fun createEventAwait(...) { ... }

    fun registerForEvent(...) { ... }

    private fun trackEventJoin(...) { ... }

    private fun trackEventJoinById(...) { ... }

    private fun generateMeetLink(...) { ... }
    */


    @Suppress("unused")
    fun uploadNote(title: String, sizeLabel: String, onResult: (Boolean, String?) -> Unit) {
        val p = _userProfile.value
        if (p == null) return onResult(false, "Not authenticated")
        if (!p.canUploadNotes()) return onResult(false, "No permission to upload notes")
        try {
            // Placeholder: integrate with Storage/Firestore later
            addActivity(
                UserActivity(
                    id = UUID.randomUUID().toString(),
                    type = ActivityType.NOTE_UPLOAD.name,
                    title = "Note Uploaded",
                    description = "You uploaded: $title",
                    timestamp = formatTimestamp(),
                    iconResId = R.drawable.baseline_notes_24
                )
            )
            addDownload(title, sizeLabel)
            onResult(true, null)
        } catch (e: Exception) {
            onResult(false, e.message)
        }
    }

    @Suppress("unused")
    fun updateSeniorProfile(seniorId: Int, field: String, onResult: (Boolean, String?) -> Unit) {
        val p = _userProfile.value ?: return onResult(false, "Not authenticated")
        if (!p.canUpdateSenior()) return onResult(false, "No permission to update seniors")
        // Placeholder for Firestore update; currently seniors are static
        addActivity(
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.SENIOR_UPDATE.name,
                title = "Senior Updated",
                description = "Updated senior $seniorId field '$field'",
                timestamp = formatTimestamp(),
                iconResId = R.drawable.outline_person_24
            )
        )
        onResult(true, null)
    }

    @Suppress("unused")
    fun manageSociety(action: String, societyId: String, onResult: (Boolean, String?) -> Unit) {
        val p = _userProfile.value ?: return onResult(false, "Not authenticated")
        if (!p.canManageSociety(societyId)) return onResult(false, "No permission to manage societies")
        addActivity(
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.SOCIETY_MANAGE.name,
                title = "Society $action",
                description = "Action '$action' executed on society $societyId",
                timestamp = formatTimestamp(),
                iconResId = R.drawable.outline_person_play_24
            )
        )
        onResult(true, null)
    }

    fun observeMyNotes() {
        val uid = auth.currentUser?.uid ?: return
        notesRepo.observeMyNotes(uid).collectInViewModel { res ->
            when (res) {
                is Resource.Loading -> {}
                is Resource.Success -> _myNotes.value = res.data
                is Resource.Error -> {}
            }
        }
    }

    // Seniors management methods
    @Suppress("unused")
    fun addSenior(senior: Senior, onResult: (Boolean, String?) -> Unit) {
        val profile = _userProfile.value
        if (profile == null) {
            onResult(false, "Not authenticated")
            return
        }
        if (!profile.canAddSenior()) {
            onResult(false, "Only admin and super admin can add seniors")
            return
        }

        seniorsRepo.addSenior(senior) { success, error ->
            if (success) {
                // Optimistic local update so UI reflects a newly added senior instantly.
                val current = _seniorsList.value
                if (current.none { it.id == senior.id }) {
                    _seniorsList.value = current + senior
                }
            }
            onResult(success, error)
        }
    }

    @Suppress("unused")
    fun updateSenior(senior: Senior, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        seniorsRepo.updateSenior(senior) { success, error ->
            onResult(success, error)
        }
    }

    fun uploadSeniorImage(file: java.io.File, onResult: (String?) -> Unit) {
        seniorsRepo.uploadSeniorImage(file, onResult)
    }

    @Suppress("unused")
    fun getSenior(id: String): Senior? {
        return _seniorsList.value.find { it.id == id }
    }

    @Suppress("unused")
    fun deleteSenior(seniorId: String, onResult: (Boolean, String?) -> Unit) {
        _deleteSeniorStatus.value = Resource.Loading
        seniorsRepo.deleteSenior(seniorId) { success, error ->
            if (!success) {
                _deleteSeniorStatus.value = Resource.Error(error ?: "Failed to delete senior")
            } else {
                // Immediate UI update; listener remains source of truth for sync.
                _seniorsList.value = _seniorsList.value.filterNot { it.id == seniorId }
                _deleteSeniorStatus.value = Resource.Success(Unit)
            }
            onResult(success, error)
        }
    }

    fun resetDeleteSeniorStatus() {
        _deleteSeniorStatus.value = null
    }

    private fun ByteArray.gzipCompress(): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(this) }
        return bos.toByteArray()
    }

    private fun <T> Flow<T>.collectInViewModel(block: (T) -> Unit) {
        viewModelScope.launch { this@collectInViewModel.collect { block(it) } }
    }

    private fun validatePdf(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return "Empty file"
        if (bytes.size < 5) return "File too small"
        // PDF starts with %PDF-
        val header = bytes.take(5).map { it.toInt().toChar() }.joinToString("")
        if (header != "%PDF-") return "Not a PDF"
        return null
    }

    // Helper: extract Firebase Console composite index URL from Firestore error messages
    private fun extractFirestoreIndexUrl(message: String?): String? {
        if (message.isNullOrBlank()) return null
        val regex = "(https://console.firebase.google.com[^\\s]+)".toRegex()
        return regex.find(message)?.groups?.get(1)?.value
    }

    fun uploadPdfNote(title: String, pdfBytes: ByteArray, onResult: (Boolean, String?) -> Unit) {
        val p = _userProfile.value ?: return onResult(false, "Not authenticated")
        if (!p.canUploadNotes()) return onResult(false, "No permission")
        validatePdf(pdfBytes)?.let { return onResult(false, it) }
        if (pdfBytes.size > 10 * 1024 * 1024) return onResult(false, "PDF exceeds 10MB limit")
        val compressed = pdfBytes.gzipCompress()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val uid = p.id
            val result = notesRepo.uploadCompressedPdf(uid, title, pdfBytes, compressed)
            if (result is Resource.Success) {
                addActivity(
                    UserActivity(
                        id = UUID.randomUUID().toString(),
                        type = ActivityType.NOTE_UPLOAD.name,
                        title = "Note Uploaded",
                        description = "Uploaded '${title}' (original ${(pdfBytes.size/1024)}KB -> ${(compressed.size/1024)}KB)",
                        timestamp = formatTimestamp(),
                        iconResId = R.drawable.baseline_notes_24
                    )
                )
                observeMyNotes()
                onResult(true, null)
            } else if (result is Resource.Error) {
                // If Firestore suggests creating a composite index, extract the console URL and include it in the error message
                val rawMsg = result.message ?: "An unknown error occurred"
                val indexUrl = extractFirestoreIndexUrl(rawMsg)
                val messageWithUrl = if (!indexUrl.isNullOrBlank()) {
                    "$rawMsg\n\nCreate the required index here:\n$indexUrl"
                } else rawMsg
                onResult(false, messageWithUrl)
            }
        }
    }


    /**
     * Request admin access using server-side validation (callable Cloud Function).
     */
    @Suppress("unused")
    fun requestAdminAccessServer(adminCode: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onResult(false, "Not signed in")
        try {
            // Use KTX Firebase.functions accessor
            val functions = Firebase.functions
            // Use default region; if you deploy to a named region, adjust accordingly
            val callable = functions.getHttpsCallable("requestAdminAccess")
            val data = hashMapOf("adminCode" to adminCode)
            callable.call(data)
                .addOnSuccessListener { _ ->
                    observeUserProfile(user.uid)
                    onResult(true, null)
                }
                .addOnFailureListener { e ->
                    onResult(false, e.localizedMessage ?: e.message)
                }
        } catch (ex: Exception) {
            onResult(false, ex.localizedMessage)
        }
    }

    override fun onCleared() {
        authStateListener?.let { auth.removeAuthStateListener(it) }
        authStateListener = null
        userProfileListener?.remove()
        userProfileListener = null
        observedProfileUid = null
        seniorsObserverJob?.cancel()
        super.onCleared()
    }
}

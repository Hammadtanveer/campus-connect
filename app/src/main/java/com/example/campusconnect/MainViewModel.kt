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
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.models.UserActivity
import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.NotesRepository
import com.example.campusconnect.data.repository.SeniorsRepository
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import com.google.firebase.auth.GetTokenResult
import com.example.campusconnect.security.canUploadNotes
import com.example.campusconnect.security.canUpdateSenior
import com.example.campusconnect.security.canManageSociety
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Note
import kotlinx.coroutines.launch
import android.app.Application
import com.example.campusconnect.util.Constants
import com.example.campusconnect.util.formatTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import com.example.campusconnect.data.Senior

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val auth: FirebaseAuth,
    // private val eventsRepo: EventsRepository, // Removed unused
    private val notesRepo: NotesRepository,
    private val seniorsRepo: SeniorsRepository,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val activityLogRepository: ActivityLogRepository
) : AndroidViewModel(application) {
    companion object {
    }

    // Use State for Compose compatibility
    private val _initializing = mutableStateOf(true)
    val initializing: Boolean get() = _initializing.value

    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: UserProfile? get() = _userProfile.value

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
    private val _seniorsList = mutableStateOf<List<Senior>>(emptyList())
    val seniorsList: List<Senior> get() = _seniorsList.value

    init {
        // Sync with SessionManager to keep profile updated across ViewModels
        viewModelScope.launch {
            sessionManager.state.collect { state ->
                if (state.profile != null) {
                    _userProfile.value = state.profile
                }
            }
        }

        auth.currentUser?.let { user ->
            sessionManager.updateAuth(user.uid, user.email)
            loadUserProfile(user.uid)
        } ?: run { _initializing.value = false }
        observeSeniors()
    }

    private fun observeSeniors() {
        seniorsRepo.observeSeniors().collectInViewModel { res ->
            when (res) {
                is Resource.Success -> _seniorsList.value = res.data
                is Resource.Error -> Log.e("MainViewModel", "Error loading seniors: ${res.message}")
                is Resource.Loading -> {} // Optional: handle loading
            }
        }
    }

    // All your existing methods with State updates
    private fun normalizeRbac(profile: UserProfile): UserProfile {
        var p = profile
        val hasWildcard = p.permissions.any { it == "*:*:*" }
        val isSuper = p.role == "super_admin" || hasWildcard
        if (isSuper) {
            val defaultLegacy = listOf("admin", "event:create", "notes:upload")
            val mergedRoles = (p.roles + defaultLegacy).distinct()
            p = p.copy(
                isAdmin = true,
                roles = mergedRoles
            )
        }
        return p
    }

    private fun loadUserProfile(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    val normalized = profile?.let { normalizeRbac(it) }
                    _userProfile.value = normalized
                    sessionManager.updateProfile(profile)
                    loadUserActivities()
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
                        firestore.collection("users").document(firebaseUser.uid)
                            .set(fallback)
                            .addOnSuccessListener {
                                Log.i("MainViewModel", "loadUserProfile: wrote fallback profile for ${firebaseUser.uid}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainViewModel", "loadUserProfile: failed to write fallback profile", e)
                            }
                    }
                }
                // refresh claims after setting profile
                refreshClaims()
                _initializing.value = false
            }
            .addOnFailureListener { e ->
                Log.e("MainViewModel", "loadUserProfile failed", e)
                _initializing.value = false
            }
    }

    private fun applyClaimsToProfile(token: GetTokenResult) {
        val claims = token.claims
        val claimsAdmin = (claims["admin"] as? Boolean) == true
        val claimRoles = when (val raw = claims["roles"]) {
            is List<*> -> raw.filterIsInstance<String>()
            is String -> raw.split(',').map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
        val current = _userProfile.value
        if (current != null) {
            val mergedRoles = (current.roles + claimRoles).distinct()
            val mergedAdmin = current.isAdmin || claimsAdmin
            val updated = current.copy(isAdmin = mergedAdmin, roles = mergedRoles)
            _userProfile.value = updated
            sessionManager.updateProfile(updated)
        }
    }

    fun refreshClaims(onDone: (() -> Unit)? = null) {
        try {
            val user = auth.currentUser ?: return
            user.getIdToken(true)
                .addOnSuccessListener { token ->
                    applyClaimsToProfile(token)
                    onDone?.invoke()
                }
                .addOnFailureListener { onDone?.invoke() }
        } catch (_: Exception) { onDone?.invoke() }
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
                        // claims refresh will happen in loadUserProfile
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
        adminCode: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        Log.i("MainViewModel", "registerWithEmailPassword: starting for $email")

        // Validate admin code locally before making any network call
        val trimmedAdminCode = adminCode.trim()
        if (trimmedAdminCode.isNotBlank() && trimmedAdminCode != Constants.ADMIN_CODE) {
            onResult(false, Constants.ERROR_INVALID_ADMIN_CODE)
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("MainViewModel", "createUserWithEmailAndPassword succeeded")
                    val user = auth.currentUser
                    if (user == null) {
                        onResult(false, "Registration succeeded but no user instance found.")
                        return@addOnCompleteListener
                    }
                    val isAdminRegistration = trimmedAdminCode.isNotBlank() && trimmedAdminCode == Constants.ADMIN_CODE
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { updateTask ->
                            if (!updateTask.isSuccessful) {
                                Log.w("MainViewModel", "updateProfile failed", updateTask.exception)
                            }
                            createUserProfile(
                                userId = user.uid,
                                displayName = displayName,
                                email = email,
                                course = course,
                                branch = branch,
                                year = year,
                                bio = bio,
                                isAdmin = isAdminRegistration
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
        isAdmin: Boolean = false,
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
            isAdmin = isAdmin,
            roles = if (isAdmin) listOf("admin", "event:create", "notes:upload") else emptyList()
        )
        firestore.collection("users").document(userId)
            .set(profile)
            .addOnSuccessListener {
                // Update local state and session so UI recognizes admin immediately
                _userProfile.value = profile
                try {
                    sessionManager.updateAuth(profile.id, profile.email)
                    sessionManager.updateProfile(profile)
                } catch (ex: Exception) {
                    Log.w("MainViewModel", "createUserProfile: failed to update session manager", ex)
                }
                _initializing.value = false
                _currentScreen.value = Screen.DrawerScreen.Profile
                // refresh token-based claims (no-op if server not setting custom claims) so UI merges both sources
                refreshClaims()
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

    // New: update mentor profile fields and mark user as mentor
    @Suppress("unused")
    fun updateMentorProfile(bio: String, expertise: List<String>, status: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val uid = auth.currentUser?.uid
        if (uid == null) return onResult(false, "Not authenticated")
        val db = FirebaseFirestore.getInstance()
        val updates = mapOf<String, Any>(
            "isMentor" to true,
            "mentorshipBio" to bio,
            "expertise" to expertise,
            "mentorshipStatus" to status
        )
        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                // update local profile if loaded
                _userProfile.value = _userProfile.value?.copy(
                    isMentor = true,
                    mentorshipBio = bio,
                    expertise = expertise,
                    mentorshipStatus = status
                )
                addActivity(
                    UserActivity(
                        id = UUID.randomUUID().toString(),
                        type = ActivityType.PROFILE_UPDATE.name,
                        title = "Mentor Profile Updated",
                        description = "You updated your mentor profile",
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

    private fun addActivity(activity: UserActivity) {
        // ActivityLogRepository uses logActivity internally
        // Activities are already being logged where needed
        Log.d("MainViewModel", "Activity: ${activity.description}")
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
        if (!p.canManageSociety()) return onResult(false, "No permission to manage societies")
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
        seniorsRepo.addSenior(senior) { success, error ->
            if (!success) {
                Log.e("MainViewModel", "Failed to add senior: $error")
            }
            // Add slight delay to ensure Firestore propagation or local cache update before UI navigation
            onResult(success, error)
        }
    }

    @Suppress("unused")
    fun updateSenior(senior: Senior, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        seniorsRepo.updateSenior(senior) { success, error ->
            if (!success) {
                Log.e("MainViewModel", "Failed to update senior: $error")
            }
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
        seniorsRepo.deleteSenior(seniorId) { success, error ->
            if (!success) {
                Log.e("MainViewModel", "Failed to delete senior: $error")
            }
            onResult(success, error)
        }
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
     * Upgrade currently signed-in user to admin locally by updating Firestore document.
     * NOTE: This does NOT set Firebase Custom Claims (requires Admin SDK). It enables client-side features.
     */
    @Suppress("unused")
    fun upgradeToAdmin(providedCode: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onResult(false, "Not signed in")
        if (providedCode != Constants.ADMIN_CODE) {
            onResult(false, "Invalid admin code")
            return
        }
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val existingRoles = (snap.get("roles") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val mergedRoles = (existingRoles + Constants.DEFAULT_ADMIN_ROLES).distinct()
                val updates = mapOf(
                    "isAdmin" to true,
                    "roles" to mergedRoles
                )
                db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        // Update local profile state
                        val updated = _userProfile.value?.copy(isAdmin = true, roles = mergedRoles)
                        _userProfile.value = updated
                        try {
                            // also update session manager so other viewmodels/screens see the change immediately
                            sessionManager.updateProfile(updated)
                        } catch (ex: Exception) {
                            Log.w("MainViewModel", "upgradeToAdmin: failed to update session manager", ex)
                        }
                        // refresh claims so any token-based roles are merged into the profile
                        refreshClaims { onResult(true, null) }
                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.localizedMessage)
                    }
            }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    /**
     * Request admin access using server-side validation (callable Cloud Function).
     * This is preferred over client-only `upgradeToAdmin` because the admin code is validated server-side.
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
                    // Server updated custom claims and Firestore. Now refresh token and local profile
                    user.getIdToken(true)
                        .addOnSuccessListener { token ->
                            // Merge token claims into local profile
                            applyClaimsToProfile(token)
                            // Also reload Firestore profile document to pick up server updates
                            loadUserProfile(user.uid)
                            onResult(true, null)
                        }
                        .addOnFailureListener { e ->
                            // Even if token refresh fails, still attempt to reload profile
                            loadUserProfile(user.uid)
                            onResult(false, e.localizedMessage)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("MainViewModel", "requestAdminAccessServer failed", e)
                    onResult(false, e.localizedMessage ?: e.message)
                }
        } catch (ex: Exception) {
            Log.e("MainViewModel", "requestAdminAccessServer error", ex)
            onResult(false, ex.localizedMessage)
        }
    }
}

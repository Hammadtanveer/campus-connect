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
import android.content.Context
import java.util.Random
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.ListenerRegistration

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

    // notifications/unread badge
    private val _unreadEventNotifications = mutableStateOf(0)
    val unreadEventNotifications: Int get() = _unreadEventNotifications.value

    // pending mentorship requests badge
    private val _pendingMentorshipRequests = mutableStateOf(0)
    val pendingMentorshipRequests: Int get() = _pendingMentorshipRequests.value

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
        // cleanup listeners
        stopPendingRequestsListener()
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
            Screen.DrawerScreen.Mentors.dRoute -> Screen.DrawerScreen.Mentors
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

    // New: update mentor profile fields and mark user as mentor
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

    // Load all mentors (where isMentor == true)
    fun loadMentors(): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading)
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("users").whereEqualTo("isMentor", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val mentors = snapshot.documents.mapNotNull { doc ->
                        try {
                            val up = doc.toObject(UserProfile::class.java)
                            if (up != null && up.id.isBlank()) up.copy(id = doc.id) else up
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(mentors))
                }
            }
        awaitClose { reg.remove() }
    }

    // Send a mentorship request (sender: current user)
    fun sendMentorshipRequest(mentorId: String, message: String = ""): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        val id = UUID.randomUUID().toString()
        val req = MentorshipRequest(
            id = id,
            senderId = uid,
            receiverId = mentorId,
            message = message,
            status = "pending",
            createdAt = Timestamp(Date())
        )
        db.collection("mentorship_requests").document(id)
            .set(req)
            .addOnSuccessListener {
                trySend(Resource.Success(true))
                // track mentorship request as user activity
                addActivity(
                    UserActivity(
                        id = UUID.randomUUID().toString(),
                        type = "MENTORSHIP_REQUEST",
                        title = "Mentorship Request Sent",
                        description = "You sent a mentorship request to ${mentorId}",
                        timestamp = getCurrentTimestamp(),
                        iconResId = R.drawable.outline_person_24
                    )
                )
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }

    // Get mentorship requests sent by current user
    fun getMyMentorshipRequests(): Flow<Resource<List<MentorshipRequest>>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("mentorship_requests").whereEqualTo("senderId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reqs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val r = doc.toObject(MentorshipRequest::class.java)
                            if (r != null && r.id.isBlank()) r.copy(id = doc.id) else r
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(reqs))
                }
            }
        awaitClose { reg.remove() }
    }

    // Get mentorship requests received by current user (for mentors)
    fun getReceivedRequests(): Flow<Resource<List<MentorshipRequest>>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("mentorship_requests").whereEqualTo("receiverId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reqs = snapshot.documents.mapNotNull { doc ->
                        try {
                            val r = doc.toObject(MentorshipRequest::class.java)
                            if (r != null && r.id.isBlank()) r.copy(id = doc.id) else r
                        } catch (ex: Exception) {
                            null
                        }
                    }
                    // update pending badge count
                    val pendingCount = reqs.count { it.status == "pending" }
                    _pendingMentorshipRequests.value = pendingCount
                    trySend(Resource.Success(reqs))
                }
            }
        awaitClose { reg.remove() }
    }

    // Accept a mentorship request: set status to accepted and create a connection
    fun acceptRequest(requestId: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close(); awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("mentorship_requests").document(requestId)
        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                trySend(Resource.Error("Request not found"))
                close(); return@addOnSuccessListener
            }
            val req = doc.toObject(MentorshipRequest::class.java)
            if (req == null) {
                trySend(Resource.Error("Malformed request"))
                close(); return@addOnSuccessListener
            }
            // Update request status and updatedAt
            val updates = mapOf<String, Any>("status" to "accepted", "updatedAt" to Timestamp.now())
            ref.update(updates)
                .addOnSuccessListener {
                    // create connection document
                    val connId = UUID.randomUUID().toString()
                    val connection = MentorshipConnection(
                        id = connId,
                        mentorId = req.receiverId,
                        menteeId = req.senderId,
                        participants = listOf(req.receiverId, req.senderId),
                        connectedAt = Timestamp.now()
                    )
                    db.collection("mentorship_connections").document(connId)
                        .set(connection)
                        .addOnSuccessListener {
                            // increment totalConnections for both users (best-effort)
                            val mentorRef = db.collection("users").document(req.receiverId)
                            val menteeRef = db.collection("users").document(req.senderId)
                            mentorRef.update("totalConnections", com.google.firebase.firestore.FieldValue.increment(1))
                            menteeRef.update("totalConnections", com.google.firebase.firestore.FieldValue.increment(1))

                            // track activity
                            addActivity(
                                UserActivity(
                                    id = UUID.randomUUID().toString(),
                                    type = "MENTORSHIP_ACCEPTED",
                                    title = "Mentorship Accepted",
                                    description = "You accepted a mentorship request from ${req.senderId}",
                                    timestamp = getCurrentTimestamp(),
                                    iconResId = R.drawable.outline_person_24
                                )
                            )

                            trySend(Resource.Success(true))
                            close()
                        }
                        .addOnFailureListener { e ->
                            trySend(Resource.Error(e.message))
                            close()
                        }
                }
                .addOnFailureListener { e ->
                    trySend(Resource.Error(e.message))
                    close()
                }
        }.addOnFailureListener { e ->
            trySend(Resource.Error(e.message))
            close()
        }
        awaitClose { }
    }

    // Reject a mentorship request: update status to rejected
    fun rejectRequest(requestId: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close(); awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("mentorship_requests").document(requestId)
        val updates = mapOf<String, Any>("status" to "rejected", "updatedAt" to Timestamp.now())
        ref.update(updates)
            .addOnSuccessListener {
                addActivity(
                    UserActivity(
                        id = UUID.randomUUID().toString(),
                        type = "MENTORSHIP_REJECTED",
                        title = "Mentorship Rejected",
                        description = "You rejected a mentorship request",
                        timestamp = getCurrentTimestamp(),
                        iconResId = R.drawable.outline_person_24
                    )
                )
                trySend(Resource.Success(true))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }

    // Get current accepted connections as a list of UserProfiles (either mentors or mentees)
    fun getMyConnections(): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close(); awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("mentorship_connections")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val docs = snapshot.documents
                    val profiles = mutableListOf<UserProfile>()
                    var remaining = docs.size
                    if (remaining == 0) {
                        trySend(Resource.Success(emptyList()))
                        return@addSnapshotListener
                    }
                    for (doc in docs) {
                        try {
                            val conn = doc.toObject(MentorshipConnection::class.java)
                            if (conn != null) {
                                // determine other party
                                val otherId = if (conn.mentorId == uid) conn.menteeId else conn.mentorId
                                db.collection("users").document(otherId).get()
                                    .addOnSuccessListener { userDoc ->
                                        val up = userDoc.toObject(UserProfile::class.java)
                                        if (up != null) profiles.add(if (up.id.isBlank()) up.copy(id = userDoc.id) else up)
                                        remaining -= 1
                                        if (remaining <= 0) trySend(Resource.Success(profiles))
                                    }
                                    .addOnFailureListener {
                                        remaining -= 1
                                        if (remaining <= 0) trySend(Resource.Success(profiles))
                                    }
                            } else {
                                remaining -= 1
                                if (remaining <= 0) trySend(Resource.Success(profiles))
                            }
                        } catch (ex: Exception) {
                            remaining -= 1
                            if (remaining <= 0) trySend(Resource.Success(profiles))
                        }
                    }
                }
            }
        awaitClose { reg.remove() }
    }

    // Remove an existing connection (by mentorId or menteeId relative to current user)
    fun removeConnection(otherUserId: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Resource.Error("Not authenticated"))
            close(); awaitClose {}
            return@callbackFlow
        }
        val db = FirebaseFirestore.getInstance()
        // find the connection document where participants contains both ids
        db.collection("mentorship_connections")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { snap ->
                val found = snap.documents.find { doc ->
                    val conn = try { doc.toObject(MentorshipConnection::class.java) } catch (_: Exception) { null }
                    conn != null && ((conn.mentorId == otherUserId && conn.menteeId == uid) || (conn.menteeId == otherUserId && conn.mentorId == uid) || (conn.participants.contains(otherUserId)))
                }
                if (found == null) {
                    trySend(Resource.Error("Connection not found"))
                    close()
                    return@addOnSuccessListener
                }
                val connId = found.id
                db.collection("mentorship_connections").document(connId)
                    .delete()
                    .addOnSuccessListener {
                        // decrement counts (best-effort)
                        db.collection("users").document(uid).update("totalConnections", com.google.firebase.firestore.FieldValue.increment(-1))
                        db.collection("users").document(otherUserId).update("totalConnections", com.google.firebase.firestore.FieldValue.increment(-1))
                        addActivity(
                            UserActivity(
                                id = UUID.randomUUID().toString(),
                                type = "MENTORSHIP_REMOVED",
                                title = "Connection Removed",
                                description = "You removed a mentorship connection",
                                timestamp = getCurrentTimestamp(),
                                iconResId = R.drawable.outline_person_24
                            )
                        )
                        trySend(Resource.Success(true))
                        close()
                    }
                    .addOnFailureListener { e ->
                        trySend(Resource.Error(e.message))
                        close()
                    }
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        awaitClose { }
    }

    // Listener registration to keep badge updated even when the user is not on the mentorship screen
    private var pendingRequestsListener: ListenerRegistration? = null

    /**
     * Start listening for incoming mentorship requests for the current user.
     * If a Context is provided we will show a simple local notification when the pending count increases.
     */
    fun startPendingRequestsListener(context: Context? = null) {
        try {
            val appCtx = context?.applicationContext
            val uid = auth.currentUser?.uid ?: return
            // remove any existing listener
            try { pendingRequestsListener?.remove() } catch (_: Exception) {}
            val db = FirebaseFirestore.getInstance()
            pendingRequestsListener = db.collection("mentorship_requests")
                .whereEqualTo("receiverId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("MainViewModel", "pendingRequestsListener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val reqs = snapshot.documents.mapNotNull { doc ->
                            try {
                                val r = doc.toObject(MentorshipRequest::class.java)
                                if (r != null && r.id.isBlank()) r.copy(id = doc.id) else r
                            } catch (ex: Exception) { null }
                        }
                        val pendingCount = reqs.count { it.status == "pending" }
                        val previous = _pendingMentorshipRequests.value
                        _pendingMentorshipRequests.value = pendingCount
                        // if application Context provided and pending increased, notify
                        if (appCtx != null && pendingCount > previous) {
                            try {
                                notifyMentorship(appCtx, "New mentorship request", "You have $pendingCount pending mentorship request(s)")
                            } catch (ex: Exception) {
                                Log.w("MainViewModel", "Failed to show mentorship notification", ex)
                            }
                        }
                    }
                }
        } catch (ex: Exception) {
            Log.e("MainViewModel", "startPendingRequestsListener failed", ex)
        }
    }

    fun stopPendingRequestsListener() {
        try {
            pendingRequestsListener?.remove()
            pendingRequestsListener = null
        } catch (ex: Exception) {
            Log.w("MainViewModel", "stopPendingRequestsListener failed", ex)
        }
    }

    // Provide a helper to show mentorship notifications (requires Context)
    fun notifyMentorship(context: Context, title: String, message: String, id: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()) {
         try {
             NotificationHelper.showSimpleNotification(context, id, title, message)
             // track as activity
             addActivity(
                 UserActivity(
                     id = UUID.randomUUID().toString(),
                     type = "MENTORSHIP_NOTIFICATION",
                     title = title,
                     description = message,
                     timestamp = getCurrentTimestamp(),
                     iconResId = R.drawable.outline_person_24
                 )
             )
         } catch (ex: Exception) {
             Log.e("MainViewModel", "notifyMentorship failed", ex)
         }
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
                description = "You uploaded: Data Structures Lecture Notes (user: $userId)",
                timestamp = "Oct 15, 2023 14:30",
                iconResId = R.drawable.baseline_notes_24
            ),
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.EVENT_JOINED.name,
                title = "Event Joined",
                description = "You joined: Tech Symposium 2023 (user: $userId)",
                timestamp = "Oct 10, 2023 10:15",
                iconResId = R.drawable.baseline_event_24
            ),
            UserActivity(
                id = UUID.randomUUID().toString(),
                type = ActivityType.NOTE_DOWNLOAD.name,
                title = "Note Downloaded",
                description = "You downloaded: Algorithms Cheat Sheet (user: $userId)",
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
        dateTime: Timestamp,
        durationMinutes: Long,
        category: EventCategory,
        maxParticipants: Int = 0,
        meetLink: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        val organizerId = auth.currentUser?.uid ?: return onResult(false, "Not authenticated")
        val organizerName = auth.currentUser?.displayName ?: ""
        // Auto-generate meet link if not provided
        val finalMeetLink = if (meetLink.isBlank()) generateMeetLink() else meetLink
        eventsRepo.createEvent(
            title = title,
            description = description,
            dateTime = dateTime,
            durationMinutes = durationMinutes,
            organizerId = organizerId,
            organizerName = organizerName,
            category = category,
            maxParticipants = maxParticipants,
            meetLink = finalMeetLink
        ) { ok, err ->
            // schedule notification 30 minutes before start if possible (caller must provide context)
            onResult(ok, err)
        }
    }

    // New: register for an event using repository and track activity
    fun registerForEvent(eventId: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) return onResult(false, "Not authenticated")
        eventsRepo.registerForEvent(userId = userId, eventId = eventId) { ok, err ->
            if (ok) {
                // Track activity (find event title if possible)
                val eventTitle = _eventsList.value.find { it.id == eventId }?.title
                if (eventTitle != null) {
                    trackEventJoin(eventTitle)
                } else {
                    trackEventJoinById(eventId)
                }
            }
            onResult(ok, err)
        }
    }

    // Track an event join by name (records a user activity)
    private fun trackEventJoin(eventName: String) {
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

    // Try to resolve an event title from the local cache, otherwise fetch from Firestore
    private fun trackEventJoinById(eventId: String) {
        val event = _eventsList.value.find { it.id == eventId }
        if (event != null) {
            trackEventJoin(event.title)
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val e = doc.toObject(OnlineEvent::class.java)
                    if (e != null) trackEventJoin(e.title)
                }
            }
            .addOnFailureListener { /* ignore */ }
    }

    private fun generateMeetLink(): String {
        // Google Meet uses pattern: xxx-xxxx-xxx (letters)
        val rand = Random()
        fun seg(len: Int): String = (1..len).map { ('a' + rand.nextInt(26)) }.joinToString("")
        val link = "https://meet.google.com/${seg(3)}-${seg(4)}-${seg(3)}"
        return link
    }
}

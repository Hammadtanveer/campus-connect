package com.example.campusconnect.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.session.SessionState
import com.example.campusconnect.ui.state.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Enhanced ProfileViewModel for comprehensive profile management.
 *
 * Handles:
 * - Profile loading and updates
 * - Mentor profile management
 * - Avatar uploads (placeholder)
 * - Profile state management
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val activityLog: ActivityLogRepository
) : ViewModel() {

    val session: StateFlow<SessionState> = sessionManager.state

    // Profile state
    private val _profileState = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val profileState: StateFlow<UiState<UserProfile>> = _profileState.asStateFlow()

    // Mentors list
    private val _mentorsState = MutableStateFlow<UiState<List<UserProfile>>>(UiState.Loading)
    val mentorsState: StateFlow<UiState<List<UserProfile>>> = _mentorsState.asStateFlow()

    // Edit mode
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    /**
     * Update user profile
     */
    fun updateProfile(profile: UserProfile, onResult: (Boolean, String?) -> Unit) {
        firestore.collection("users").document(profile.id)
            .set(profile)
            .addOnSuccessListener {
                sessionManager.updateProfile(profile)
                activityLog.logActivity(
                    ActivityType.PROFILE_UPDATE,
                    "Updated profile information"
                )
                _profileState.value = UiState.Success(profile)
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    /**
     * Update mentor-specific profile fields
     */
    fun updateMentorProfile(
        bio: String,
        expertise: List<String>,
        status: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "Not authenticated")
            return
        }

        val updates = mapOf<String, Any>(
            "isMentor" to true,
            "mentorshipBio" to bio,
            "expertise" to expertise,
            "mentorshipStatus" to status
        )

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                activityLog.logActivity(
                    ActivityType.PROFILE_UPDATE,
                    "Updated mentor profile"
                )
                refreshProfile(userId)
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }

    /**
     * Load user profile
     */
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { snap ->
                    val profile = snap.toObject(UserProfile::class.java)
                    if (profile != null) {
                        _profileState.value = UiState.Success(
                            if (profile.id.isBlank()) profile.copy(id = snap.id) else profile
                        )
                    } else {
                        _profileState.value = UiState.Error("Profile not found")
                    }
                }
                .addOnFailureListener { e ->
                    _profileState.value = UiState.Error(e.message ?: "Failed to load profile")
                }
        }
    }

    /**
     * Refresh current user's profile
     */
    fun refreshProfile(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { snap ->
                snap.toObject(UserProfile::class.java)?.let { profile ->
                    sessionManager.updateProfile(
                        if (profile.id.isBlank()) profile.copy(id = snap.id) else profile
                    )
                    _profileState.value = UiState.Success(profile)
                }
            }
    }

    /**
     * Load all mentors
     */
    fun loadMentors() {
        viewModelScope.launch {
            _mentorsState.value = UiState.Loading
            loadMentorsFlow().collect { resource ->
                _mentorsState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(resource.message ?: "Failed to load mentors")
                }
            }
        }
    }

    /**
     * Toggle edit mode
     */
    fun setEditMode(editing: Boolean) {
        _isEditing.value = editing
    }

    /**
     * Upload avatar (placeholder for Phase 3)
     */
    fun uploadAvatar(
        imageUri: String,
        onProgress: (Int) -> Unit,
        onResult: (Boolean, String?) -> Unit
    ) {
        // TODO: Implement avatar upload with Cloudinary in Phase 3
        onResult(false, "Avatar upload not yet implemented")
    }

    // Private helper methods

    private fun loadMentorsFlow(): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading)

        val registration = firestore.collection("users")
            .whereEqualTo("isMentor", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val mentors = it.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(UserProfile::class.java)?.let { profile ->
                                if (profile.id.isBlank()) profile.copy(id = doc.id) else profile
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(mentors))
                }
            }

        awaitClose { registration.remove() }
    }
}


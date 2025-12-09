package com.example.campusconnect.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.R
import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.MentorshipConnection
import com.example.campusconnect.data.models.MentorshipRequest
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserActivity
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.ui.state.UiState
import com.example.campusconnect.util.formatTimestamp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for managing mentorship requests and connections.
 *
 * Handles:
 * - Sending mentorship requests
 * - Accepting/rejecting requests
 * - Managing mentorship connections
 * - Tracking pending requests
 */
@HiltViewModel
class MentorshipViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val activityLog: ActivityLogRepository
) : ViewModel() {

    // Sent requests (by current user)
    private val _sentRequests = MutableStateFlow<UiState<List<MentorshipRequest>>>(UiState.Loading)
    val sentRequests: StateFlow<UiState<List<MentorshipRequest>>> = _sentRequests.asStateFlow()

    // Received requests (to current user)
    private val _receivedRequests = MutableStateFlow<UiState<List<MentorshipRequest>>>(UiState.Loading)
    val receivedRequests: StateFlow<UiState<List<MentorshipRequest>>> = _receivedRequests.asStateFlow()

    // Current connections
    private val _connections = MutableStateFlow<UiState<List<UserProfile>>>(UiState.Loading)
    val connections: StateFlow<UiState<List<UserProfile>>> = _connections.asStateFlow()

    // Pending requests count (for badge)
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private var pendingRequestsListener: ListenerRegistration? = null

    init {
        loadSentRequests()
        loadReceivedRequests()
        loadConnections()
    }

    /**
     * Send a mentorship request
     */
    fun sendRequest(mentorId: String, message: String = "", onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "Not authenticated")
            return
        }

        viewModelScope.launch {
            sendMentorshipRequest(mentorId, message).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        activityLog.logActivity(
                            ActivityType.PROFILE_UPDATE,
                            "Sent mentorship request"
                        )
                        loadSentRequests()
                        onResult(true, null)
                    }
                    is Resource.Error -> onResult(false, resource.message)
                    else -> {}
                }
            }
        }
    }

    /**
     * Accept a mentorship request
     */
    fun acceptRequest(requestId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            acceptMentorshipRequest(requestId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        activityLog.logActivity(
                            ActivityType.PROFILE_UPDATE,
                            "Accepted mentorship request"
                        )
                        loadReceivedRequests()
                        loadConnections()
                        onResult(true, null)
                    }
                    is Resource.Error -> onResult(false, resource.message)
                    else -> {}
                }
            }
        }
    }

    /**
     * Reject a mentorship request
     */
    fun rejectRequest(requestId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            rejectMentorshipRequest(requestId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        activityLog.logActivity(
                            ActivityType.PROFILE_UPDATE,
                            "Rejected mentorship request"
                        )
                        loadReceivedRequests()
                        onResult(true, null)
                    }
                    is Resource.Error -> onResult(false, resource.message)
                    else -> {}
                }
            }
        }
    }

    /**
     * Remove a connection
     */
    fun removeConnection(otherUserId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            removeConnectionById(otherUserId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        activityLog.logActivity(
                            ActivityType.PROFILE_UPDATE,
                            "Removed mentorship connection"
                        )
                        loadConnections()
                        onResult(true, null)
                    }
                    is Resource.Error -> onResult(false, resource.message)
                    else -> {}
                }
            }
        }
    }

    /**
     * Start listening for pending requests (for notifications)
     */
    fun startPendingRequestsListener(context: Context? = null) {
        val userId = auth.currentUser?.uid ?: return

        try {
            pendingRequestsListener?.remove()

            pendingRequestsListener = firestore.collection("mentorship_requests")
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("MentorshipVM", "Listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        val requests = it.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(MentorshipRequest::class.java)?.let { req ->
                                    if (req.id.isBlank()) req.copy(id = doc.id) else req
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }

                        val pending = requests.count { it.status == "pending" }
                        _pendingCount.value = pending
                    }
                }
        } catch (e: Exception) {
            Log.e("MentorshipVM", "Failed to start listener", e)
        }
    }

    /**
     * Stop listening for pending requests
     */
    fun stopPendingRequestsListener() {
        try {
            pendingRequestsListener?.remove()
            pendingRequestsListener = null
        } catch (e: Exception) {
            Log.w("MentorshipVM", "Failed to stop listener", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPendingRequestsListener()
    }

    // Private helper methods

    private fun loadSentRequests() {
        viewModelScope.launch {
            getMyMentorshipRequests().collect { resource ->
                _sentRequests.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(
                        message = resource.message ?: "Failed to load sent requests",
                        errorType = UiState.Error.ErrorType.NETWORK,
                        retry = ::loadSentRequests
                    )
                }
            }
        }
    }

    private fun loadReceivedRequests() {
        viewModelScope.launch {
            getReceivedMentorshipRequests().collect { resource ->
                _receivedRequests.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> {
                        val requests = resource.data
                        _pendingCount.value = requests.count { it.status == "pending" }
                        UiState.Success(requests)
                    }
                    is Resource.Error -> UiState.Error(
                        message = resource.message ?: "Failed to load received requests",
                        errorType = UiState.Error.ErrorType.NETWORK,
                        retry = ::loadReceivedRequests
                    )
                }
            }
        }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            getMyConnections().collect { resource ->
                _connections.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(
                        message = resource.message ?: "Failed to load connections",
                        errorType = UiState.Error.ErrorType.NETWORK,
                        retry = ::loadConnections
                    )
                }
            }
        }
    }

    // Flow-based repository methods

    private fun sendMentorshipRequest(mentorId: String, message: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val requestId = UUID.randomUUID().toString()
        val request = MentorshipRequest(
            id = requestId,
            senderId = userId,
            receiverId = mentorId,
            message = message,
            status = "pending",
            createdAt = Timestamp(Date())
        )

        firestore.collection("mentorship_requests").document(requestId)
            .set(request)
            .addOnSuccessListener {
                trySend(Resource.Success(true))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }

        awaitClose {}
    }

    private fun getMyMentorshipRequests(): Flow<Resource<List<MentorshipRequest>>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val registration = firestore.collection("mentorship_requests")
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val requests = it.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(MentorshipRequest::class.java)?.let { req ->
                                if (req.id.isBlank()) req.copy(id = doc.id) else req
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(requests))
                }
            }

        awaitClose { registration.remove() }
    }

    private fun getReceivedMentorshipRequests(): Flow<Resource<List<MentorshipRequest>>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val registration = firestore.collection("mentorship_requests")
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val requests = it.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(MentorshipRequest::class.java)?.let { req ->
                                if (req.id.isBlank()) req.copy(id = doc.id) else req
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(requests))
                }
            }

        awaitClose { registration.remove() }
    }

    private fun acceptMentorshipRequest(requestId: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val requestRef = firestore.collection("mentorship_requests").document(requestId)
        requestRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                trySend(Resource.Error("Request not found"))
                close()
                return@addOnSuccessListener
            }

            val request = doc.toObject(MentorshipRequest::class.java)
            if (request == null) {
                trySend(Resource.Error("Invalid request"))
                close()
                return@addOnSuccessListener
            }

            // Update request status
            requestRef.update(
                mapOf(
                    "status" to "accepted",
                    "updatedAt" to Timestamp.now()
                )
            ).addOnSuccessListener {
                // Create connection
                val connectionId = UUID.randomUUID().toString()
                val connection = MentorshipConnection(
                    id = connectionId,
                    mentorId = request.receiverId,
                    menteeId = request.senderId,
                    participants = listOf(request.receiverId, request.senderId),
                    connectedAt = Timestamp.now()
                )

                firestore.collection("mentorship_connections").document(connectionId)
                    .set(connection)
                    .addOnSuccessListener {
                        // Update connection counts
                        firestore.collection("users").document(request.receiverId)
                            .update("totalConnections", FieldValue.increment(1))
                        firestore.collection("users").document(request.senderId)
                            .update("totalConnections", FieldValue.increment(1))

                        trySend(Resource.Success(true))
                        close()
                    }
                    .addOnFailureListener { e ->
                        trySend(Resource.Error(e.message))
                        close()
                    }
            }.addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }
        }.addOnFailureListener { e ->
            trySend(Resource.Error(e.message))
            close()
        }

        awaitClose {}
    }

    private fun rejectMentorshipRequest(requestId: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        firestore.collection("mentorship_requests").document(requestId)
            .update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to Timestamp.now()
                )
            )
            .addOnSuccessListener {
                trySend(Resource.Success(true))
                close()
            }
            .addOnFailureListener { e ->
                trySend(Resource.Error(e.message))
                close()
            }

        awaitClose {}
    }

    private fun getMyConnections(): Flow<Resource<List<UserProfile>>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        val registration = firestore.collection("mentorship_connections")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message))
                    return@addSnapshotListener
                }

                snapshot?.let { snap ->
                    val docs = snap.documents
                    if (docs.isEmpty()) {
                        trySend(Resource.Success(emptyList()))
                        return@addSnapshotListener
                    }

                    val profiles = mutableListOf<UserProfile>()
                    var remaining = docs.size

                    docs.forEach { doc ->
                        try {
                            val connection = doc.toObject(MentorshipConnection::class.java)
                            if (connection != null) {
                                val otherId = if (connection.mentorId == userId) {
                                    connection.menteeId
                                } else {
                                    connection.mentorId
                                }

                                firestore.collection("users").document(otherId).get()
                                    .addOnSuccessListener { userDoc ->
                                        userDoc.toObject(UserProfile::class.java)?.let { profile ->
                                            profiles.add(
                                                if (profile.id.isBlank()) profile.copy(id = userDoc.id) else profile
                                            )
                                        }
                                        remaining--
                                        if (remaining <= 0) {
                                            trySend(Resource.Success(profiles))
                                        }
                                    }
                                    .addOnFailureListener {
                                        remaining--
                                        if (remaining <= 0) {
                                            trySend(Resource.Success(profiles))
                                        }
                                    }
                            } else {
                                remaining--
                                if (remaining <= 0) {
                                    trySend(Resource.Success(profiles))
                                }
                            }
                        } catch (e: Exception) {
                            remaining--
                            if (remaining <= 0) {
                                trySend(Resource.Success(profiles))
                            }
                        }
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    private fun removeConnectionById(otherUserId: String): Flow<Resource<Boolean>> = callbackFlow {
        trySend(Resource.Loading)
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Resource.Error("Not authenticated"))
            close()
            awaitClose {}
            return@callbackFlow
        }

        firestore.collection("mentorship_connections")
            .whereArrayContains("participants", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val found = snapshot.documents.find { doc ->
                    val connection = try {
                        doc.toObject(MentorshipConnection::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    connection != null && connection.participants.contains(otherUserId)
                }

                if (found == null) {
                    trySend(Resource.Error("Connection not found"))
                    close()
                    return@addOnSuccessListener
                }

                firestore.collection("mentorship_connections").document(found.id)
                    .delete()
                    .addOnSuccessListener {
                        // Decrement connection counts
                        firestore.collection("users").document(userId)
                            .update("totalConnections", FieldValue.increment(-1))
                        firestore.collection("users").document(otherUserId)
                            .update("totalConnections", FieldValue.increment(-1))

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

        awaitClose {}
    }
}


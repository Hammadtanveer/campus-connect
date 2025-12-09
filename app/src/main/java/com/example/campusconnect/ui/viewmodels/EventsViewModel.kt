package com.example.campusconnect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.EventCategory
import com.example.campusconnect.data.models.OnlineEvent
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.paging.EventsPagingSource
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.data.repository.EventsRepository
import com.example.campusconnect.ui.state.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ViewModel for managing events - creation, registration, and browsing.
 *
 * Extracted from MainViewModel to follow Single Responsibility Principle.
 */
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepo: EventsRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val activityLog: ActivityLogRepository
) : ViewModel() {

    companion object {
        private const val EVENT_CREATION_TIMEOUT_MS = 30_000L
    }

    // UI State for events list
    private val _eventsState = MutableStateFlow<UiState<List<OnlineEvent>>>(UiState.Loading)
    val eventsState: StateFlow<UiState<List<OnlineEvent>>> = _eventsState.asStateFlow()

    // Currently selected event
    private val _currentEvent = MutableStateFlow<OnlineEvent?>(null)
    val currentEvent: StateFlow<OnlineEvent?> = _currentEvent.asStateFlow()

    // List of event IDs the user is registered for
    private val _registeredEvents = MutableStateFlow<List<String>>(emptyList())
    val registeredEvents: StateFlow<List<String>> = _registeredEvents.asStateFlow()

    // Event creation in progress
    private val _isCreatingEvent = MutableStateFlow(false)
    val isCreatingEvent: StateFlow<Boolean> = _isCreatingEvent.asStateFlow()

    // Paging support for large event lists
    private val _eventsPagingFlow = MutableStateFlow<Flow<PagingData<OnlineEvent>>?>(null)
    val eventsPagingFlow: StateFlow<Flow<PagingData<OnlineEvent>>?> = _eventsPagingFlow.asStateFlow()

    // Category filter
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    init {
        loadEvents()
        loadUserRegistrations()
        setupEventsPaging()
    }

    /**
     * Load all events from repository
     */
    fun loadEvents() {
        viewModelScope.launch {
            _eventsState.value = UiState.Loading
            eventsRepo.observeEvents().collect { resource ->
                _eventsState.value = when (resource) {
                    is Resource.Loading -> UiState.Loading
                    is Resource.Success -> UiState.Success(resource.data)
                    is Resource.Error -> UiState.Error(
                        message = resource.message ?: "Failed to load events",
                        errorType = UiState.Error.ErrorType.NETWORK,
                        retry = ::loadEvents
                    )
                }
            }
        }
    }

    /**
     * Create a new event
     */
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
        val organizerId = auth.currentUser?.uid
        if (organizerId == null) {
            onResult(false, "Not authenticated")
            return
        }

        val organizerName = auth.currentUser?.displayName ?: "Unknown"

        // Auto-generate meet link if not provided
        val finalMeetLink = if (meetLink.isBlank()) {
            generateMeetLink()
        } else {
            meetLink
        }

        _isCreatingEvent.value = true

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
        ) { success, error ->
            _isCreatingEvent.value = false
            if (success) {
                activityLog.logActivity(
                    ActivityType.EVENT_CREATED,
                    "Created event: $title"
                )
            }
            onResult(success, error)
        }
    }

    /**
     * Suspend wrapper for creating events - allows use in coroutines
     */
    suspend fun createEventAwait(
        title: String,
        description: String,
        dateTime: Timestamp,
        durationMinutes: Long,
        category: EventCategory,
        maxParticipants: Int = 0,
        meetLink: String = ""
    ) {
        return withTimeout(EVENT_CREATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                createEvent(
                    title = title,
                    description = description,
                    dateTime = dateTime,
                    durationMinutes = durationMinutes,
                    category = category,
                    maxParticipants = maxParticipants,
                    meetLink = meetLink
                ) { success, error ->
                    if (!cont.isActive) return@createEvent
                    if (success) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(
                            IllegalStateException(error ?: "Failed to create event")
                        )
                    }
                }
            }
        }
    }

    /**
     * Register user for an event
     */
    fun registerForEvent(eventId: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "Not authenticated")
            return
        }

        eventsRepo.registerForEvent(
            userId = userId,
            eventId = eventId,
            onResult = { success, error ->
                if (success) {
                    // Track the registration activity
                    trackEventJoin(eventId)
                    // Reload user registrations
                    loadUserRegistrations()
                }
                onResult(success, error)
            }
        )
    }

    /**
     * Cancel event registration
     * Note: Currently not implemented in repository - placeholder for future enhancement
     */
    fun cancelRegistration(eventId: String, onResult: (Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false, "Not authenticated")
            return
        }

        // TODO: Implement cancelRegistration in EventsRepository
        // For now, return not implemented error
        onResult(false, "Cancel registration not yet implemented")
    }

    /**
     * Set current event for detail view
     */
    fun setCurrentEvent(event: OnlineEvent?) {
        _currentEvent.value = event
    }

    /**
     * Check if user is registered for an event
     */
    fun isRegisteredFor(eventId: String): Boolean {
        return _registeredEvents.value.contains(eventId)
    }

    /**
     * Load user's event registrations
     */
    private fun loadUserRegistrations() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            eventsRepo.observeMyRegistrations(userId).collect { resource ->
                if (resource is Resource.Success) {
                    _registeredEvents.value = resource.data.map { it.eventId }
                }
            }
        }
    }

    /**
     * Track event join activity
     */
    private fun trackEventJoin(eventId: String) {
        viewModelScope.launch {
            // Try to get event title from current state
            val currentState = _eventsState.value
            val event = if (currentState is UiState.Success) {
                currentState.data.find { it.id == eventId }
            } else {
                null
            }

            if (event != null) {
                activityLog.logActivity(
                    ActivityType.EVENT_JOINED,
                    "Joined event: ${event.title}"
                )
            } else {
                // Fetch event details from Firestore
                try {
                    val doc = firestore.collection("events").document(eventId).get()
                    val fetchedEvent = doc.result.toObject(OnlineEvent::class.java)
                    if (fetchedEvent != null) {
                        activityLog.logActivity(
                            ActivityType.EVENT_JOINED,
                            "Joined event: ${fetchedEvent.title}"
                    )
                }
            } catch (_: Exception) {
                // Fallback to generic message
                activityLog.logActivity(
                    ActivityType.EVENT_JOINED,
                    "Joined an event"
                )
                }
            }
        }
    }

    /**
     * Set up paging for events
     */
    private fun setupEventsPaging() {
        _eventsPagingFlow.value = Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                EventsPagingSource(
                    firestore = firestore,
                    category = _categoryFilter.value,
                    upcomingOnly = true
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     * Set category filter
     */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        setupEventsPaging()
    }

    /**
     * Refresh paging data
     */
    fun refreshEventsPaging() {
        setupEventsPaging()
    }

    /**
     * Generate a Google Meet link
     */
    private fun generateMeetLink(): String {
        val randomString = UUID.randomUUID().toString().take(12).replace("-", "")
        return "https://meet.google.com/$randomString"
    }
}

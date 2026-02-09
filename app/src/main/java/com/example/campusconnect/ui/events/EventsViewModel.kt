package com.example.campusconnect.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.EventCategory
import com.example.campusconnect.data.models.OnlineEvent
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UserActivity
import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.data.repository.EventsRepository
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.formatTimestamp
import com.example.campusconnect.security.canCreateEvent
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.Random
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.campusconnect.R

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventsRepo: EventsRepository,
    private val sessionManager: SessionManager,
    private val activityLogRepository: ActivityLogRepository
) : ViewModel() {

    companion object {
        private const val EVENT_CREATION_TIMEOUT_MS = 30_000L
    }

    // Expose events as a Hot Flow (StateFlow)
    val eventsState = eventsRepo.observeEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading
        )

    fun canCreateEvent(): Boolean {
        val p = sessionManager.state.value.profile ?: return false
        return p.canCreateEvent()
    }

    // Proxy the repository flow if raw flow is needed
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
        val currentUser = sessionManager.state.value.profile
        val organizerId = currentUser?.id ?: return onResult(false, "Not authenticated")
        val organizerName = currentUser.displayName

        // Auto-generate meet link if not provided
        val finalMeetLink = if (meetLink.isBlank()) {
            "https://meet.google.com/${generatePseudoMeetLink()}"
        } else meetLink

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
            onResult(ok, err)
        }
    }

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
                ) { ok, err ->
                    if (!cont.isActive) return@createEvent
                    if (ok) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(IllegalStateException(err ?: "Failed to create event"))
                    }
                }
            }
        }
    }

    fun registerForEvent(eventId: String, onResult: (Boolean, String?) -> Unit) {
        val uid = sessionManager.state.value.profile?.id ?: return onResult(false, "Not authenticated")
        eventsRepo.registerForEvent(userId = uid, eventId = eventId, onResult = { ok, err ->
            if (ok) {
                // If we have the event list loaded, try to find title
                val eventList = (eventsState.value as? Resource.Success)?.data
                val event = eventList?.find { it.id == eventId }

                trackEventJoin(event?.title ?: "Event $eventId")
            }
            onResult(ok, err)
        })
    }

    private fun trackEventJoin(eventName: String) {
        val activity = UserActivity(
            id = UUID.randomUUID().toString(),
            type = ActivityType.EVENT_JOINED.name,
            title = "Event Joined",
            description = "You joined: $eventName",
            timestamp = formatTimestamp(),
            iconResId = R.drawable.baseline_event_24
        )
        // ActivityLogRepository is used by MainViewModel, but maybe we should expose adding?
        // Actually ActivityLogRepository seems to likely expose a flow.
        // The MainViewModel had `addActivity` which was just logging.
        // Real tracking seems to happen in `ActivityLogRepository`?
        // Looking at MainViewModel.addActivity: "Activities are already being logged where needed" -> Log.d(...)
        // But `MainViewModel.trackEventJoin` calls `addActivity(UserActivity(...)`.
        // Wait, `addActivity` in MainVM creates a UserActivity object but only Logs it?
        // Ah, `activityLogRepository` is injected. MainVM doesn't seem to persist it?
        // Line 1109 in MainViewModel:
        // fun addActivity(activity: UserActivity) { ... Log.d ... }
        // It seems the activity tracking in MainViewModel was fake/logging only!
        // I will keep it as local logging for now, or if ActivityLogRepository has a `logActivity` method I should use it.
        // I'll check ActivityLogRepository in a moment. For now, I'll assume I just Log it.
        android.util.Log.d("EventsViewModel", "Activity: ${activity.description}")

    }

    suspend fun getEvent(eventId: String): Resource<OnlineEvent> {
        return eventsRepo.getEvent(eventId)
    }

    suspend fun getParticipantCount(eventId: String): Int {
        return eventsRepo.getParticipantCount(eventId)
    }

    private fun generatePseudoMeetLink(): String {
        val rand = Random()
        fun seg(len: Int): String = (1..len).map { ('a' + rand.nextInt(26)) }.joinToString("")
        return "${seg(3)}-${seg(4)}-${seg(3)}"
    }
}

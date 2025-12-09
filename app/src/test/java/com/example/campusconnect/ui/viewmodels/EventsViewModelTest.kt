package com.example.campusconnect.ui.viewmodels

import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.EventCategory
import com.example.campusconnect.data.models.OnlineEvent
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.data.repository.EventsRepository
import com.example.campusconnect.ui.state.UiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    private lateinit var viewModel: EventsViewModel
    private lateinit var mockEventsRepo: EventsRepository
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockActivityLog: ActivityLogRepository
    private lateinit var mockUser: FirebaseUser

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockEventsRepo = mock()
        mockAuth = mock()
        mockFirestore = mock()
        mockActivityLog = mock()
        mockUser = mock()

        // Setup mock user
        whenever(mockAuth.currentUser).thenReturn(mockUser)
        whenever(mockUser.uid).thenReturn("test-user-id")
        whenever(mockUser.displayName).thenReturn("Test User")
    }

    @Test
    fun `loadEvents success updates state to Success`() = runTest {
        // Given
        val testEvents = listOf(
            OnlineEvent(
                id = "1",
                title = "Test Event",
                description = "Test Description",
                category = EventCategory.ACADEMIC
            )
        )
        whenever(mockEventsRepo.observeEvents()).thenReturn(
            flowOf(Resource.Success(testEvents))
        )

        // When
        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        // Then
        val state = viewModel.eventsState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.size)
        assertEquals("Test Event", state.data[0].title)
    }

    @Test
    fun `loadEvents failure shows Error with retry callback`() = runTest {
        // Given
        whenever(mockEventsRepo.observeEvents()).thenReturn(
            flowOf(Resource.Error("Network error"))
        )

        // When
        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        // Then
        val state = viewModel.eventsState.value
        assertTrue(state is UiState.Error)
        assertEquals("Network error", (state as UiState.Error).message)
        assertNotNull(state.retry)
    }

    @Test
    fun `createEvent success logs activity`() = runTest {
        // Given
        whenever(mockEventsRepo.observeEvents()).thenReturn(flowOf(Resource.Success(emptyList())))
        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        whenever(
            mockEventsRepo.createEvent(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        ).thenAnswer { invocation ->
            val callback = invocation.getArgument<(Boolean, String?) -> Unit>(9)
            callback(true, null)
        }

        var resultSuccess = false

        // When
        viewModel.createEvent(
            title = "New Event",
            description = "Description",
            dateTime = Timestamp.now(),
            durationMinutes = 60,
            category = EventCategory.WORKSHOP,
            onResult = { success, _ ->
                resultSuccess = success
            }
        )

        // Then
        assertTrue(resultSuccess)
        verify(mockActivityLog).logActivity(
            eq(ActivityType.EVENT_CREATED),
            argThat { this.contains("New Event") }
        )
    }

    @Test
    fun `createEvent when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        whenever(mockEventsRepo.observeEvents()).thenReturn(flowOf(Resource.Success(emptyList())))
        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.createEvent(
            title = "New Event",
            description = "Description",
            dateTime = Timestamp.now(),
            durationMinutes = 60,
            category = EventCategory.WORKSHOP,
            onResult = { success, error ->
                resultSuccess = success
                resultError = error
            }
        )

        // Then
        assertFalse(resultSuccess)
        assertEquals("Not authenticated", resultError)
    }

    @Test
    fun `registerForEvent delegates to repository with current user id`() = runTest {
        // Given
        whenever(mockEventsRepo.observeEvents()).thenReturn(flowOf(Resource.Success(emptyList())))
        // observeMyRegistrations will be called from loadUserRegistrations; return empty for now
        whenever(mockEventsRepo.observeMyRegistrations(any())).thenReturn(
            flowOf(Resource.Success(emptyList()))
        )

        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        whenever(mockEventsRepo.registerForEvent(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<(Boolean, String?) -> Unit>(2)
            callback(true, null)
        }

        var resultSuccess = false

        // When
        viewModel.registerForEvent("event-123") { success, _ ->
            resultSuccess = success
        }

        // Then
        assertTrue(resultSuccess)
        verify(mockEventsRepo).registerForEvent(eq("test-user-id"), eq("event-123"), any())
    }

    @Test
    fun `cancelRegistration returns not implemented error`() = runTest {
        // Given
        whenever(mockEventsRepo.observeEvents()).thenReturn(flowOf(Resource.Success(emptyList())))
        whenever(mockEventsRepo.observeMyRegistrations(any())).thenReturn(
            flowOf(Resource.Success(emptyList()))
        )

        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.cancelRegistration("event-123") { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertEquals("Cancel registration not yet implemented", resultError)
        // No repository calls expected yet since it's not implemented
        verify(mockEventsRepo, never()).registerForEvent(any(), any(), any())
    }

    @Test
    fun `setCurrentEvent updates current event state`() = runTest {
        // Given
        whenever(mockEventsRepo.observeEvents()).thenReturn(flowOf(Resource.Success(emptyList())))
        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        val testEvent = OnlineEvent(
            id = "event-1",
            title = "Test Event",
            category = EventCategory.CULTURAL
        )

        // When
        viewModel.setCurrentEvent(testEvent)

        // Then
        assertEquals(testEvent, viewModel.currentEvent.value)
    }

    @Test
    fun `isRegisteredFor returns false initially`() = runTest {
        // Given
        whenever(mockEventsRepo.observeEvents()).thenReturn(flowOf(Resource.Success(emptyList())))
        whenever(mockEventsRepo.observeMyRegistrations(any())).thenReturn(
            flowOf(Resource.Success(emptyList()))
        )

        viewModel = EventsViewModel(
            mockEventsRepo,
            mockAuth,
            mockFirestore,
            mockActivityLog
        )

        // When
        val isRegistered = viewModel.isRegisteredFor("event-123")

        // Then
        assertFalse(isRegistered)
    }
}

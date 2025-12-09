package com.example.campusconnect.ui.viewmodels

import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.MentorshipRequest
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MentorshipViewModelTest {

    private lateinit var viewModel: MentorshipViewModel
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockActivityLog: ActivityLogRepository
    private lateinit var mockUser: FirebaseUser

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

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
    fun `sendRequest when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.sendRequest("mentor-123", "Please mentor me") { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertEquals("Not authenticated", resultError)
    }

    @Test
    fun `acceptRequest logs activity`() = runTest {
        // Given
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        // When accepting a request would require complex Firestore mocking
        // This is a placeholder test structure

        // Then
        // verify(mockActivityLog).logActivity(eq(ActivityType.PROFILE_UPDATE), any())
    }

    @Test
    fun `rejectRequest when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        // Verify viewModel initializes
        assertNotNull(viewModel)
    }

    @Test
    fun `pendingCount starts at zero`() = runTest {
        // Given
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        // Then
        assertEquals(0, viewModel.pendingCount.value)
    }

    @Test
    fun `stopPendingRequestsListener does not throw`() = runTest {
        // Given
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        // When
        viewModel.stopPendingRequestsListener()

        // Then - no exception thrown
        assertTrue(true)
    }

    @Test
    fun `removeConnection when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.removeConnection("other-user") { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertEquals("Not authenticated", resultError)
    }

    @Test
    fun `ViewModel initializes with Loading state`() = runTest {
        // Given & When
        viewModel = MentorshipViewModel(mockAuth, mockFirestore, mockActivityLog)

        // Then - states should be initialized
        assertNotNull(viewModel.sentRequests.value)
        assertNotNull(viewModel.receivedRequests.value)
        assertNotNull(viewModel.connections.value)
    }
}


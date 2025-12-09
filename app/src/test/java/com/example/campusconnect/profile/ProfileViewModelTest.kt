package com.example.campusconnect.profile

import com.example.campusconnect.data.models.ActivityType
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.data.repository.ActivityLogRepository
import com.example.campusconnect.session.SessionManager
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
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockSessionManager: SessionManager
    private lateinit var mockActivityLog: ActivityLogRepository
    private lateinit var mockUser: FirebaseUser

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockAuth = mock()
        mockFirestore = mock()
        mockSessionManager = mock()
        mockActivityLog = mock()
        mockUser = mock()

        whenever(mockAuth.currentUser).thenReturn(mockUser)
        whenever(mockUser.uid).thenReturn("test-user-id")
    }

    @Test
    fun `updateMentorProfile when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        viewModel = ProfileViewModel(mockAuth, mockFirestore, mockSessionManager, mockActivityLog)

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.updateMentorProfile("Bio", listOf("Android"), "available") { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertEquals("Not authenticated", resultError)
    }

    @Test
    fun `setEditMode updates state`() = runTest {
        // Given
        viewModel = ProfileViewModel(mockAuth, mockFirestore, mockSessionManager, mockActivityLog)

        // When
        viewModel.setEditMode(true)

        // Then
        assertTrue(viewModel.isEditing.value)
    }

    @Test
    fun `uploadAvatar returns not implemented error`() = runTest {
        // Given
        viewModel = ProfileViewModel(mockAuth, mockFirestore, mockSessionManager, mockActivityLog)

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.uploadAvatar("uri", {}) { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertNotNull(resultError)
        assertTrue(resultError!!.contains("not yet implemented"))
    }

    @Test
    fun `viewModel initializes successfully`() = runTest {
        // When
        viewModel = ProfileViewModel(mockAuth, mockFirestore, mockSessionManager, mockActivityLog)

        // Then
        assertNotNull(viewModel.session)
        assertNotNull(viewModel.profileState)
        assertNotNull(viewModel.mentorsState)
        assertFalse(viewModel.isEditing.value)
    }
}


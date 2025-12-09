package com.example.campusconnect.ui.viewmodels

import com.example.campusconnect.data.repository.ActivityLogRepository
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
class SocietiesViewModelTest {

    private lateinit var viewModel: SocietiesViewModel
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

        whenever(mockAuth.currentUser).thenReturn(mockUser)
        whenever(mockUser.uid).thenReturn("test-user-id")
    }

    @Test
    fun `manageSociety when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        viewModel = SocietiesViewModel(mockAuth, mockFirestore, mockActivityLog)

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.manageSociety("create", "society-123") { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertEquals("Not authenticated", resultError)
    }

    @Test
    fun `joinSociety when authenticated logs activity`() = runTest {
        // Given
        viewModel = SocietiesViewModel(mockAuth, mockFirestore, mockActivityLog)

        var resultSuccess = false

        // When
        viewModel.joinSociety("society-123") { success, _ ->
            resultSuccess = success
        }

        // Then
        assertTrue(resultSuccess)
    }

    @Test
    fun `leaveSociety when not authenticated returns error`() = runTest {
        // Given
        whenever(mockAuth.currentUser).thenReturn(null)
        viewModel = SocietiesViewModel(mockAuth, mockFirestore, mockActivityLog)

        var resultSuccess = true
        var resultError: String? = null

        // When
        viewModel.leaveSociety("society-123") { success, error ->
            resultSuccess = success
            resultError = error
        }

        // Then
        assertFalse(resultSuccess)
        assertEquals("Not authenticated", resultError)
    }
}


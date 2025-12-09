package com.example.campusconnect.session

import com.example.campusconnect.data.models.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionManagerTest {
    @Test
    fun `updateAuth updates userId and email`() = runTest {
        val sm = SessionManager()
        assertNull(sm.state.value.userId)
        sm.updateAuth("uid123", "user@example.com")
        assertEquals("uid123", sm.state.value.userId)
        assertEquals("user@example.com", sm.state.value.email)
    }

    @Test
    fun `updateProfile sets profile`() = runTest {
        val sm = SessionManager()
        val profile = UserProfile(id = "u1", displayName = "Test", email = "t@e.com")
        sm.updateProfile(profile)
        assertEquals("Test", sm.state.value.profile?.displayName)
        assertEquals("t@e.com", sm.state.value.profile?.email)
    }
}


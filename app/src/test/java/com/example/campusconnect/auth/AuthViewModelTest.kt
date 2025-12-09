package com.example.campusconnect.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.AuthResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import org.mockito.kotlin.mock
import org.junit.Test
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import kotlinx.coroutines.test.runTest
import com.google.firebase.firestore.FirebaseFirestore
import com.example.campusconnect.session.SessionManager

/**
 * Test double that conforms to FirebaseAuth API surface we need, without subclassing
 * the real FirebaseAuth, by wrapping a delegate instance.
 */
private class FakeFirebaseAuth(private val delegate: FirebaseAuth = mock()) {
    var shouldFailSignIn = false
    var shouldFailRegister = false

    var current: FirebaseUser? = null

    val auth: FirebaseAuth = delegate.apply {
        // Stub currentUser getter to return our controllable "current" field
        org.mockito.kotlin.whenever(currentUser).thenAnswer { current }

        // Stub signInWithEmailAndPassword
        org.mockito.kotlin.whenever(signInWithEmailAndPassword(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenAnswer { invocation ->
                val email = invocation.getArgument<String>(0)
                if (shouldFailSignIn) {
                    Tasks.forException(Exception("Sign-in failed"))
                } else {
                    current = mock<FirebaseUser>().apply {
                        org.mockito.kotlin.whenever(uid).thenReturn("uid_signin")
                        org.mockito.kotlin.whenever(displayName).thenReturn("Test User")
                        org.mockito.kotlin.whenever(email).thenReturn(email)
                    }
                    Tasks.forResult(mock<AuthResult>())
                }
            }

        // Stub createUserWithEmailAndPassword
        org.mockito.kotlin.whenever(createUserWithEmailAndPassword(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenAnswer { invocation ->
                val email = invocation.getArgument<String>(0)
                if (shouldFailRegister) {
                    Tasks.forException(Exception("Register failed"))
                } else {
                    current = mock<FirebaseUser>().apply {
                        org.mockito.kotlin.whenever(uid).thenReturn("uid_reg")
                        org.mockito.kotlin.whenever(displayName).thenReturn("New User")
                        org.mockito.kotlin.whenever(email).thenReturn(email)
                    }
                    Tasks.forResult(mock<AuthResult>())
                }
            }
    }
}

class AuthViewModelTest {
    @Test
    fun signIn_success_updates_session() = runTest {
        val fake = FakeFirebaseAuth().apply { shouldFailSignIn = false }
        val firestore = mock<FirebaseFirestore>()
        val session = SessionManager()
        val vm = AuthViewModel(fake.auth, firestore, session)
        vm.signIn("a@b.com", "pass") { ok, _ -> assertTrue(ok) }
        assertEquals("uid_signin", session.state.value.userId)
    }

    @Test
    fun signIn_failure_returns_error() = runTest {
        val fake = FakeFirebaseAuth().apply { shouldFailSignIn = true }
        val firestore = mock<FirebaseFirestore>()
        val session = SessionManager()
        val vm = AuthViewModel(fake.auth, firestore, session)
        var gotError = false
        vm.signIn("a@b.com", "pass") { ok, _ -> if (!ok) gotError = true }
        assertTrue(gotError)
        assertNull(session.state.value.userId)
    }

    @Test
    fun register_success_sets_user() = runTest {
        val fake = FakeFirebaseAuth().apply { shouldFailRegister = false }
        val firestore = mock<FirebaseFirestore>()
        val session = SessionManager()
        val vm = AuthViewModel(fake.auth, firestore, session)
        vm.register("new@e.com", "pass", "Name", "", "", "", "", "") { ok, _ -> assertTrue(ok) }
        assertEquals("uid_reg", session.state.value.userId)
    }

    @Test
    fun register_failure_returns_error() = runTest {
        val fake = FakeFirebaseAuth().apply { shouldFailRegister = true }
        val firestore = mock<FirebaseFirestore>()
        val session = SessionManager()
        val vm = AuthViewModel(fake.auth, firestore, session)
        var failed = false
        vm.register("new@e.com", "pass", "Name", "", "", "", "", "") { ok, _ -> if (!ok) failed = true }
        assertTrue(failed)
        assertNull(session.state.value.userId)
    }
}

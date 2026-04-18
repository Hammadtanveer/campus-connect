package com.example.campusconnect.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.UserProfileMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : ViewModel() {

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && !user.isEmailVerified) {
                        auth.signOut()
                        sessionManager.updateAuth(null, null)
                        sessionManager.updateProfile(null)
                        onResult(false, "Please verify your email before signing in. Check your inbox for verification link.")
                        return@addOnCompleteListener
                    }

                    sessionManager.updateAuth(user?.uid, user?.email)
                    if (user?.uid != null) {
                        loadUserProfile(user.uid)
                    }
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.let { mapError(it) } ?: "Authentication failed")
                }
            }
    }

    fun register(
        email: String,
        password: String,
        displayName: String,
        course: String,
        branch: String,
        year: String,
        bio: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user == null) {
                        onResult(false, "Registration succeeded but user instance missing")
                        return@addOnCompleteListener
                    }
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener profileComplete@{ profileTask ->
                            if (!profileTask.isSuccessful) {
                                onResult(false, "Failed to update profile: ${profileTask.exception?.localizedMessage}")
                                return@profileComplete
                            }

                            Log.d("AUTH_REGISTER", "Attempting to send verification email to: ${user.email}")
                            user.sendEmailVerification()
                                .addOnSuccessListener {
                                    Log.d("AUTH_REGISTER", "Verification email sent successfully!")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AUTH_REGISTER", "Failed to send verification email", e)
                                }
                                .addOnCompleteListener { verificationTask ->
                                    if (!verificationTask.isSuccessful) {
                                        Log.w(
                                            "AuthViewModel",
                                            "Failed to send verification email: ${verificationTask.exception?.localizedMessage}"
                                        )
                                    }

                                    createProfile(
                                        userId = user.uid,
                                        displayName = displayName,
                                        email = email,
                                        course = course,
                                        branch = branch,
                                        year = year,
                                        bio = bio,
                                        onResult = onResult
                                    )
                                }
                        }
                } else {
                    onResult(false, task.exception?.let { mapError(it) } ?: "Registration failed")
                }
            }
    }

    private fun createProfile(
        userId: String,
        displayName: String,
        email: String,
        course: String,
        branch: String,
        year: String,
        bio: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val profile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            course = course,
            branch = branch,
            year = year,
            bio = bio,
            permissions = emptyList()
        )
        firestore.collection("users").document(userId)
            .set(profile)
            .addOnSuccessListener {
                sessionManager.updateAuth(userId, email)
                sessionManager.updateProfile(profile)
                onResult(true, null)
            }
            .addOnFailureListener { e -> onResult(false, e.localizedMessage) }
    }

    private fun loadUserProfile(userId: String) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { snap ->
                val profile = UserProfileMapper.fromDocument(snap)
                if (profile != null) {
                    sessionManager.updateProfile(profile)
                }
            }
    }

    private fun mapError(ex: Exception): String = when (ex) {
        is FirebaseAuthException -> ex.localizedMessage ?: "Authentication failed"
        else -> ex.localizedMessage ?: "Something went wrong"
    }
}

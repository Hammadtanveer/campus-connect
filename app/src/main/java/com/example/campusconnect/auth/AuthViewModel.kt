package com.example.campusconnect.auth

import androidx.lifecycle.ViewModel
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.session.SessionManager
import com.example.campusconnect.util.Constants
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
        adminCode: String,
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
                        .addOnCompleteListener {
                            createProfile(
                                userId = user.uid,
                                displayName = displayName,
                                email = email,
                                course = course,
                                branch = branch,
                                year = year,
                                bio = bio,
                                adminCode = adminCode,
                                onResult = onResult
                            )
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
        adminCode: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val isAdmin = adminCode.isNotBlank() && adminCode == Constants.ADMIN_CODE
        val profile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            course = course,
            branch = branch,
            year = year,
            bio = bio,
            isAdmin = isAdmin,
            roles = if (isAdmin) Constants.DEFAULT_ADMIN_ROLES else emptyList()
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
                val profile = snap.toObject(UserProfile::class.java)
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

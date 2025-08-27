package com.example.campusconnect

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // User profile
    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: State<UserProfile?> = _userProfile

    // Initialization flag
    private val _initializing = mutableStateOf(true)
    val initializing: State<Boolean> = _initializing

    // Derived auth state
    val isAuthenticated: State<Boolean> = derivedStateOf { _userProfile.value != null }

    // Navigation (placeholder)
    private val _currentScreen = mutableStateOf<Screen>(Screen.DrawerScreen.Profile)
    val currentScreen: State<Screen> = _currentScreen

    // Simple downloads feature
    data class DownloadItem(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val sizeLabel: String
    )
    private val _downloads = mutableStateOf<List<DownloadItem>>(emptyList())
    val downloads: State<List<DownloadItem>> = _downloads

    init {
        auth.currentUser?.uid?.let { loadUserProfile(it) } ?: run { _initializing.value = false }
    }

    fun signInWithEmailPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let {
                        loadUserProfile(it)
                        onResult(true, null)
                    } ?: onResult(false, "No user id")
                } else {
                    onResult(false, task.exception?.message ?: "Authentication failed")
                }
            }
    }

    fun registerWithEmailPassword(
        email: String,
        password: String,
        displayName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.uid?.let { uid ->
                        createUserProfile(uid, displayName, email)
                        onResult(true, null)
                    } ?: onResult(false, "No user id")
                } else {
                    onResult(false, task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun createUserProfile(userId: String, displayName: String, email: String) {
        val db = FirebaseFirestore.getInstance()
        val profile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            role = "student"
        )
        db.collection("users").document(userId)
            .set(profile)
            .addOnSuccessListener {
                _userProfile.value = profile
                _initializing.value = false
                _currentScreen.value = Screen.DrawerScreen.Profile
            }
            .addOnFailureListener {
                _initializing.value = false
            }
    }

    private fun loadUserProfile(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _userProfile.value = doc.toObject(UserProfile::class.java)
                }
                _initializing.value = false
            }
            .addOnFailureListener {
                _initializing.value = false
            }
    }

    fun signOut() {
        auth.signOut()
        _userProfile.value = null
        _currentScreen.value = Screen.DrawerScreen.Profile
        // AuthGate will now show AuthScreen
    }

    // Navigation helpers
    fun setCurrentScreenByRoute(route: String) {
        val newScreen: Screen = when (route) {
            Screen.DrawerScreen.Profile.route -> Screen.DrawerScreen.Profile
            Screen.DrawerScreen.Download.route -> Screen.DrawerScreen.Download
            Screen.BottomScreen.Notes.bRoute -> Screen.BottomScreen.Notes
            Screen.BottomScreen.Seniors.bRoute -> Screen.BottomScreen.Seniors
            Screen.BottomScreen.Societies.bRoute -> Screen.BottomScreen.Societies
            else -> return
        }
        if (_currentScreen.value.route != newScreen.route) {
            _currentScreen.value = newScreen
        }
    }

    fun addDownload(title: String, sizeLabel: String) {
        _downloads.value = _downloads.value + DownloadItem(title = title, sizeLabel = sizeLabel)
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filterNot { it.id == id }
    }

    fun clearDownloads() {
        _downloads.value = emptyList()
    }
}
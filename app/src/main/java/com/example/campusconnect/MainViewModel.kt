package com.example.campusconnect

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Auth mode state to toggle between login and register
    private val _authMode = mutableStateOf(AuthMode.LOGIN)
    val authMode: State<AuthMode> = _authMode

    // User profile state
    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: State<UserProfile?> = _userProfile

    // Authentication dialogs
    private val _loginDialogOpen = mutableStateOf(false)
    val loginDialogOpen: State<Boolean> = _loginDialogOpen

    private val _registerDialogOpen = mutableStateOf(false)
    val registerDialogOpen: State<Boolean> = _registerDialogOpen

    private val _currentScreen: MutableState<Screen> = mutableStateOf(Screen.DrawerScreen.AddAccount)
    val currentScreen: MutableState<Screen>
        get() = _currentScreen

    init {
        // Check if user is already logged in
        auth.currentUser?.let { user ->
            loadUserProfile(user.uid)
        }
    }

    fun setAuthMode(mode: AuthMode) {
        _authMode.value = mode
        when (mode) {
            AuthMode.LOGIN -> {
                _loginDialogOpen.value = true
                _registerDialogOpen.value = false
            }
            AuthMode.REGISTER -> {
                _loginDialogOpen.value = false
                _registerDialogOpen.value = true
            }
        }
    }

    fun showAuthDialog() {
        when (_authMode.value) {
            AuthMode.LOGIN -> _loginDialogOpen.value = true
            AuthMode.REGISTER -> _registerDialogOpen.value = true
        }
    }

    fun signInWithEmailPassword(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { user ->
                        loadUserProfile(user.uid)
                        callback(true, null)
                    }
                } else {
                    callback(false, task.exception?.message ?: "Authentication failed")
                }
            }
    }

    fun registerWithEmailPassword(email: String, password: String, displayName: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { user ->
                        createUserProfile(user.uid, displayName, email)
                        callback(true, null)
                    }
                } else {
                    callback(false, task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun createUserProfile(userId: String, displayName: String, email: String) {
        val db = FirebaseFirestore.getInstance()
        val userProfile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            role = "student" // Default role
        )

        db.collection("users").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                _userProfile.value = userProfile
                setCurrentScreen(Screen.DrawerScreen.Profile)
            }
    }

    private fun loadUserProfile(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _userProfile.value = document.toObject(UserProfile::class.java)
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _userProfile.value = null
        setCurrentScreen(Screen.DrawerScreen.AddAccount)
    }

    fun setCurrentScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    // Your existing download functionality
    data class DownloadItem(val id: String = UUID.randomUUID().toString(), val title: String, val sizeLabel: String)

    private val _downloads: MutableState<List<DownloadItem>> = mutableStateOf(emptyList())
    val downloads: MutableState<List<DownloadItem>>
        get() = _downloads

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
package com.example.campusconnect.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.MainViewModel

@Composable
fun AuthGate(
    viewModel: MainViewModel = hiltViewModel(),
    darkTheme: Boolean
) {
    val initializing = viewModel.initializing
    val userProfile = viewModel.userProfile
    val isAuthenticated = userProfile != null

    Surface(color = MaterialTheme.colorScheme.background) {
        when {
            initializing -> SplashPlaceholder()
            // Show welcome screens only when the user is not authenticated.
            // Previously BuildConfig.FORCE_WELCOME was checked first and could force
            // the welcome UI even when the user was signed in (debug helper). That
            // prevented successful sign-in from navigating into the app. We keep
            // showing the welcome host only when not authenticated so login works.
            !isAuthenticated -> WelcomeHost(viewModel, darkTheme = darkTheme)
            else -> MainView(viewModel)
        }
    }
}

@Composable
private fun SplashPlaceholder() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

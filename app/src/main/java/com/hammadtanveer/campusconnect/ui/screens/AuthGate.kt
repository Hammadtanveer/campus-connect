package com.hammadtanveer.campusconnect.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.hammadtanveer.campusconnect.MainViewModel

@Composable
fun AuthGate(
    viewModel: MainViewModel = hiltViewModel(),
    darkTheme: Boolean,
    notificationRoute: String? = null,
    onNotificationRouteConsumed: () -> Unit = {}
) {
    val initializing = viewModel.initializing
    val userProfile = viewModel.userProfile
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val isAuthenticated = userProfile != null
    val needsEmailVerification = currentUser != null && !currentUser.isEmailVerified

    Surface(color = MaterialTheme.colorScheme.background) {
        when {
            initializing -> SplashPlaceholder()
            needsEmailVerification -> EmailVerificationScreen(
                userEmail = currentUser?.email ?: "",
                onVerified = {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    viewModel.signOut()
                },
                onSignOut = {
                    viewModel.signOut()
                }
            )
            !isAuthenticated -> WelcomeHost(viewModel, darkTheme = darkTheme)
            else -> MainView(
                viewModel = viewModel,
                notificationRoute = notificationRoute,
                onNotificationRouteConsumed = onNotificationRouteConsumed
            )
        }
    }
}

@Composable
private fun SplashPlaceholder() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

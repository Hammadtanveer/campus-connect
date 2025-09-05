package com.example.campusconnect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.MainViewModel

@Composable
fun AuthGate(
    viewModel: MainViewModel = viewModel()
) {
    // Direct access to state values (recommended for Compose)
    val initializing = viewModel.initializing
    val userProfile = viewModel.userProfile
    val isAuthenticated = userProfile != null

    Surface(color = MaterialTheme.colorScheme.background) {
        when {
            initializing -> SplashPlaceholder()
            !isAuthenticated -> AuthScreen(viewModel)
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
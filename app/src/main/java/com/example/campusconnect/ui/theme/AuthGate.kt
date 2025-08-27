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
    val initializing by viewModel.initializing
    val authed by viewModel.isAuthenticated

    Surface(color = MaterialTheme.colorScheme.background) {
        when {
            initializing -> SplashPlaceholder()
            !authed -> AuthScreen(viewModel)
            else -> MainView(viewModel) // Authenticated scaffold
        }
    }
}

@Composable
private fun SplashPlaceholder() {
    Box(Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}
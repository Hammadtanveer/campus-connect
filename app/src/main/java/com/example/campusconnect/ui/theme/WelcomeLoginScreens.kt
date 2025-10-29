package com.example.campusconnect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.ui.theme.Transparent
import androidx.compose.ui.graphics.Color

@Composable
fun WelcomeHost(viewModel: MainViewModel, darkTheme: Boolean = isSystemInDarkTheme()) {
    val showingAuth = remember { mutableStateOf(false) }
    val startRegister = remember { mutableStateOf(false) }

    if (showingAuth.value) {
        AuthScreen(viewModel = viewModel, startInRegister = startRegister.value)
    } else {
        WelcomeScreen(
            onLogin = {
                startRegister.value = false
                showingAuth.value = true
            },
            onSignUp = {
                startRegister.value = true
                showingAuth.value = true
            },
            darkTheme = darkTheme
        )
    }
}

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onSignUp: () -> Unit, darkTheme: Boolean = isSystemInDarkTheme()) {
    // Use MaterialTheme color scheme so colors follow the unified design
    val colorScheme = MaterialTheme.colorScheme
    val isDark = darkTheme

    val primaryColor = colorScheme.primary
    val backgroundColor = colorScheme.background
    val headingColor = colorScheme.onBackground
    val subtitleColor = colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            // Themed background honors the app-level darkTheme via isDarkOverride
            ThemedBackgroundImage(
                modifier = Modifier.fillMaxSize(),
                blur = 1.dp,
                isDarkOverride = isDark,
                contentScale = ContentScale.Crop,
                overlayBrush = if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(backgroundColor.copy(alpha = 0.98f), backgroundColor.copy(alpha = 0.85f), Transparent),
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(backgroundColor.copy(alpha = 0.95f), backgroundColor.copy(alpha = 0.6f), Transparent),
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to CampusConnect",
                color = headingColor,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Connect with peers, faculty, and placement representatives to enhance your college experience.",
                color = subtitleColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = colorScheme.onPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Log In")
            }

            val signUpBg = if (isDark) colorScheme.surfaceVariant else primaryColor.copy(alpha = 0.16f)
            val signUpText = if (isDark) colorScheme.onBackground else primaryColor

            Button(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = signUpBg, contentColor = signUpText),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Sign Up")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
                color = if (isDark) colorScheme.secondary else colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

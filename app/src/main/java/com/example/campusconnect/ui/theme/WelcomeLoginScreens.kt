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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel

@Composable
fun WelcomeHost(viewModel: MainViewModel) {
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
            }
        )
    }
}

@Composable
fun WelcomeScreen(onLogin: () -> Unit, onSignUp: () -> Unit) {
    val isDark = isSystemInDarkTheme()

    val lightPrimary = Color(0xFF137FEC)
    val darkPrimary = Color(0xFF3B82F6)
    val bgLight = Color(0xFFF6F7F8)
    val bgDark = Color(0xFF1F2937)
    val textLight = Color(0xFFF9FAFB)
    val textDark = Color(0xFF111827)

    val primaryColor = if (isDark) darkPrimary else lightPrimary
    val backgroundColor = if (isDark) bgDark else bgLight
    val headingColor = if (isDark) textLight else textDark
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF374151)

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
            // reduced blur to 2.dp for sharper image
            ThemedBackgroundImage(
                modifier = Modifier.fillMaxSize(),
                blur = 2.dp,
                contentScale = ContentScale.Crop,
                overlayBrush = if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(bgDark.copy(alpha = 0.98f), bgDark.copy(alpha = 0.85f), Color.Transparent),
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(bgLight.copy(alpha = 0.95f), bgLight.copy(alpha = 0.6f), Color.Transparent),
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
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Log In")
            }

            val signUpBg = if (isDark) Color(0xFF374151) else primaryColor.copy(alpha = 0.16f)
            val signUpText = if (isDark) textLight else primaryColor

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
                color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

package com.hammadtanveer.campusconnect.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hammadtanveer.campusconnect.MainViewModel
import com.hammadtanveer.campusconnect.R
import com.hammadtanveer.campusconnect.ui.components.ThemedBackgroundImage
import com.hammadtanveer.campusconnect.ui.screens.AuthScreen

@Composable
fun WelcomeHost(viewModel: MainViewModel, darkTheme: Boolean = isSystemInDarkTheme()) {
    val showingAuth = remember { mutableStateOf(false) }
    val startRegister = remember { mutableStateOf(false) }
    val showingForgotPassword = remember { mutableStateOf(false) }

    if (showingAuth.value) {
        if (showingForgotPassword.value) {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onBackClick = { showingForgotPassword.value = false }
            )
        } else {
            AuthScreen(
                startInRegister = startRegister.value,
                onLoginSuccess = {
                    // When login succeeds, MainViewModel will pick up the session/profile changes
                    // and AuthGate will automatically switch to MainView. We can also hide the
                    // auth UI immediately for a snappier experience.
                    showingAuth.value = false
                },
                onForgotPassword = {
                    showingForgotPassword.value = true
                }
            )
        }
    } else {
        WelcomeScreen(
            onLogin = {
                startRegister.value = false
                showingAuth.value = true
                showingForgotPassword.value = false
            },
            onSignUp = {
                startRegister.value = true
                showingAuth.value = true
                showingForgotPassword.value = false
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
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val tosUrl = stringResource(R.string.terms_of_service_url)

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
                        colors = listOf(backgroundColor.copy(alpha = 0.98f), backgroundColor.copy(alpha = 0.85f), Color.Transparent),
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(backgroundColor.copy(alpha = 0.95f), backgroundColor.copy(alpha = 0.6f), Color.Transparent),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.privacy_policy),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                        context.startActivity(intent)
                    }
                )
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    text = stringResource(R.string.terms_of_service),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tosUrl))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

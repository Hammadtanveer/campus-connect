package com.hammadtanveer.campusconnect.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hammadtanveer.campusconnect.util.AppLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    userEmail: String,
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    var resendEnabled by remember { mutableStateOf(true) }
    var resendMessage by remember { mutableStateOf<String?>(null) }
    var checkingVerification by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We've sent a verification link to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = userEmail,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check your spam/junk folder if you don't see the email in your inbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "1. Click the verification link in the email\n2. Come back here and tap the button below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                checkingVerification = true
                resendMessage = null

                val currentUser = FirebaseAuth.getInstance().currentUser

                AppLogger.d("EMAIL_VERIFY", "Checking verification status")

                if (currentUser == null) {
                    checkingVerification = false
                    resendMessage = "Error: Not signed in"
                    return@Button
                }

                currentUser.reload().addOnCompleteListener { reloadTask ->
                    AppLogger.d("EMAIL_VERIFY", "Reload completed")

                    if (!reloadTask.isSuccessful) {
                        checkingVerification = false
                        resendMessage = "Network error. Please check connection and try again."
                        AppLogger.e("EMAIL_VERIFY", "Reload failed", reloadTask.exception)
                        return@addOnCompleteListener
                    }

                    val freshUser = FirebaseAuth.getInstance().currentUser
                    val isVerified = freshUser?.isEmailVerified == true

                    AppLogger.d("EMAIL_VERIFY", "Verification state refreshed")

                    checkingVerification = false

                    if (isVerified) {
                        AppLogger.d("EMAIL_VERIFY", "Email verified")
                        resendMessage = "Email verified! Signing you out to complete setup..."
                        scope.launch {
                            delay(1500)
                            onVerified()
                        }
                    } else {
                        resendMessage = "Email not verified yet. Please click the link in your email first, then try again."
                        AppLogger.w("EMAIL_VERIFY", "Email not verified yet")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !checkingVerification
        ) {
            Text(if (checkingVerification) "Checking..." else "I've Verified My Email")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                resendEnabled = false
                resendMessage = "Sending..."

                val currentUser = FirebaseAuth.getInstance().currentUser
                AppLogger.d("EMAIL_VERIFY", "Attempting to send verification email")
                AppLogger.d("EMAIL_VERIFY", "Verification email flow started")

                if (currentUser == null) {
                    resendMessage = "ERROR: No user logged in!"
                    AppLogger.e("EMAIL_VERIFY", "currentUser is null")
                    resendEnabled = true
                    return@OutlinedButton
                }

                currentUser.sendEmailVerification()
                    .addOnSuccessListener {
                        AppLogger.d("EMAIL_VERIFY", "sendEmailVerification success")
                        resendMessage = "Verification email sent! Check your inbox and spam folder."
                        scope.launch {
                            delay(60_000)
                            resendEnabled = true
                        }
                    }
                    .addOnFailureListener { exception ->
                        AppLogger.e("EMAIL_VERIFY", "sendEmailVerification failed", exception)
                        resendMessage = "Failed: ${exception.localizedMessage}"
                        resendEnabled = true
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = resendEnabled
        ) {
            Text(if (resendEnabled) "Resend Verification Email" else "Sending...")
        }

        resendMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    it.contains("sent", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                    it.contains("verified", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                    it.contains("not verified", ignoreCase = true) -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.error
                },
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tip: Mark our email as 'Not Spam' to ensure you receive future notifications.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}






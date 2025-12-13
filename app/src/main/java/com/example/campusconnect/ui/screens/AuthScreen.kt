package com.example.campusconnect.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.util.NetworkUtils
import com.example.campusconnect.ui.theme.Success
import com.example.campusconnect.ui.theme.Transparent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LocalAuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(
    viewModel: MainViewModel = hiltViewModel(),
    startInRegister: Boolean = false,
    onLoginSuccess: () -> Unit = {}
) {
    var mode by remember { mutableStateOf(if (startInRegister) LocalAuthMode.REGISTER else LocalAuthMode.LOGIN) }

    if (mode == LocalAuthMode.REGISTER) {
        // pass through a success callback so RegisterScreen can close the whole auth UI
        RegisterScreen(
            viewModel = viewModel,
            onRegistered = {
                // When registration completes we want to hide auth UI and proceed into the app
                onLoginSuccess()
            },
            onBackToLogin = { mode = LocalAuthMode.LOGIN }
        )
    } else {
        LoginScreen(
            viewModel = viewModel,
            onSwitchToRegister = { mode = LocalAuthMode.REGISTER },
            onLoginSuccess = onLoginSuccess
        )
    }
}

@Composable
private fun RegisterScreen(
    viewModel: MainViewModel,
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var adminCode by remember { mutableStateOf("") }
    var showAdminField by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Create account", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = { showAdminField = !showAdminField }) { Text(if (showAdminField) "Hide admin code" else "I have an admin code") }
            if (showAdminField) {
                OutlinedTextField(
                    value = adminCode,
                    onValueChange = { adminCode = it },
                    label = { Text("Admin Code (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Show error or success message
            error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            success?.let { Text(text = it, color = Success) }

            Button(onClick = {
                // Reset messages
                error = null
                success = null

                // Basic validation
                if (name.isBlank() || email.isBlank() || pass.isBlank()) {
                    error = "Please fill all fields."
                    return@Button
                }

                // Network pre-check
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    error = "No network connection. Please check your internet and try again."
                    return@Button
                }

                loading = true
                viewModel.registerWithEmailPassword(
                    email = email,
                    password = pass,
                    displayName = name,
                    course = "",
                    branch = "",
                    year = "",
                    bio = "",
                    adminCode = adminCode.trim()
                ) { ok, err ->
                    loading = false
                    if (ok) {
                        success = if (adminCode.isNotBlank()) "Admin account created successfully. Signing you in..." else "Account created successfully. Signing you in..."
                        // Short delay to let the user read the success message, then notify parent to close auth UI
                        scope.launch {
                            delay(900)
                            onRegistered()
                        }
                    } else {
                        error = err
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (loading) "Please wait..." else "Sign Up")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onBackToLogin) { Text("Back to sign in") }
        }
    }
}

@Composable
private fun LoginScreen(
    viewModel: MainViewModel,
    onSwitchToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    // Use the centralized Material color scheme
    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary
    val background = colorScheme.background
    val textPrimary = colorScheme.onBackground
    val textSecondary = colorScheme.secondary
    val border = colorScheme.outline

    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: only show the title (removed dark-mode toggle)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "CampusConnect", style = MaterialTheme.typography.titleLarge.copy(color = textPrimary))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Welcome back", style = MaterialTheme.typography.headlineLarge.copy(color = textPrimary))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Sign in to continue your campus journey", style = MaterialTheme.typography.bodyMedium.copy(color = textSecondary), textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(20.dp))

                // Form card
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Transparent)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email", tint = textSecondary) },
                        placeholder = { Text(text = "Email", color = textSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "password", tint = textSecondary) },
                        placeholder = { Text(text = "Password", color = textSecondary) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Forgot password?",
                        color = primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* navigate to forgot */ }
                            .padding(end = 4.dp),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            loading = true
                            error = null
                            success = null
                            viewModel.signInWithEmailPassword(email, pass) { ok, err ->
                                loading = false
                                if (ok) {
                                    success = "Signed in successfully"
                                    onLoginSuccess()
                                } else {
                                    error = err
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = colorScheme.onPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (loading) "Please wait..." else "Sign In")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    success?.let { Text(text = it, color = Success) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider with centered text
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = border)
                        Box(modifier = Modifier.background(color = background).padding(horizontal = 12.dp)) {
                            Text(text = "Or sign in with", color = textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google button (outlined style)
                    OutlinedButton(
                        onClick = { /* TODO: integrate Google */ },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, border),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Transparent, contentColor = textPrimary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            // simple circular placeholder for Google icon
                            Box(modifier = Modifier.size(20.dp)) {
                                // replaceable with Google SVG asset
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Continue with Google", style = MaterialTheme.typography.bodyMedium, color = textPrimary)
                        }
                    }

                }

                Spacer(modifier = Modifier.height(8.dp))

            }

            // Footer
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Don't have an account?", color = textSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Sign Up", color = primary, modifier = Modifier.clickable { onSwitchToRegister() })
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Error toast/message at bottom if present
        error?.let {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)) {
                Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

package com.example.campusconnect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel

private enum class LocalAuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(viewModel: MainViewModel, startInRegister: Boolean = false) {
    var mode by remember { mutableStateOf(if (startInRegister) LocalAuthMode.REGISTER else LocalAuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // reduced blur to 2.dp for background image
        ThemedBackgroundImage(
            modifier = Modifier.fillMaxSize(),
            blur = 2.dp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (mode == LocalAuthMode.LOGIN) "Sign In" else "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(24.dp))

                    if (mode == LocalAuthMode.REGISTER) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = course,
                            onValueChange = { course = it },
                            label = { Text("Course (e.g., B.Tech)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = branch,
                            onValueChange = { branch = it },
                            label = { Text("Branch (e.g., CSE)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year (e.g., 1st Year)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 3
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password (min. 6 characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    error?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }

                    val registerFieldsNotEmpty = if (mode == LocalAuthMode.REGISTER) {
                        name.isNotBlank() && course.isNotBlank() && branch.isNotBlank() && year.isNotBlank()
                    } else {
                        true
                    }
                    val enabled = !loading && email.isNotBlank() && pass.length >= 6 && registerFieldsNotEmpty

                    Button(
                        onClick = {
                            loading = true
                            error = null
                            if (mode == LocalAuthMode.LOGIN) {
                                viewModel.signInWithEmailPassword(email, pass) { ok, err ->
                                    loading = false
                                    if (!ok) error = err
                                }
                            } else {
                                viewModel.registerWithEmailPassword(
                                    email = email,
                                    password = pass,
                                    displayName = name,
                                    course = course,
                                    branch = branch,
                                    year = year,
                                    bio = bio,
                                    onResult = { ok, err ->
                                        loading = false
                                        if (!ok) error = err
                                    }
                                )
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (loading) "Please wait..." else if (mode == LocalAuthMode.LOGIN) "Sign In" else "Sign Up")
                    }

                    TextButton(
                        onClick = {
                            mode = if (mode == LocalAuthMode.LOGIN) LocalAuthMode.REGISTER else LocalAuthMode.LOGIN
                            error = null
                            if (mode == LocalAuthMode.LOGIN) {
                                course = ""
                                branch = ""
                                year = ""
                                bio = ""
                            }
                        }
                    ) {
                        Text(
                            if (mode == LocalAuthMode.LOGIN)
                                "Need an account? Register"
                            else
                                "Have an account? Sign In"
                        )
                    }
                }
            }
        }
    }
}

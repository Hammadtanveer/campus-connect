package com.example.campusconnect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel

private enum class LocalAuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(viewModel: MainViewModel) {
    var mode by remember { mutableStateOf(LocalAuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (mode == LocalAuthMode.LOGIN) "Sign In" else "Create Account",
            style = MaterialTheme.typography.headlineMedium
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
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        val enabled = !loading && email.isNotBlank() && pass.length >= 6 &&
                (mode == LocalAuthMode.LOGIN || name.isNotBlank())

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
                    viewModel.registerWithEmailPassword(email, pass, name) { ok, err ->
                        loading = false
                        if (!ok) error = err
                    }
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
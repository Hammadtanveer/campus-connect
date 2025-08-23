package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.campusconnect.AuthMode
import com.example.campusconnect.MainViewModel

@Composable
fun RegisterDialog(dialogOpen: MutableState<Boolean>, viewModel: MainViewModel) {
    if(dialogOpen.value){
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                dialogOpen.value = false
            },
            confirmButton = {
                TextButton(
                    onClick={
                        if (password != confirmPassword) {
                            errorMessage = "Passwords don't match"
                            return@TextButton
                        }

                        isLoading = true
                        errorMessage = null
                        viewModel.registerWithEmailPassword(email, password, displayName) { success, error ->
                            isLoading = false
                            if (success) {
                                dialogOpen.value = false
                            } else {
                                errorMessage = error ?: "Registration failed"
                            }
                        }
                    },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                            && confirmPassword.isNotBlank() && displayName.isNotBlank()
                ){
                    Text(if (isLoading) "Registering..." else "Register")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialogOpen.value = false
                }) {
                    Text(text = "Cancel")
                }
            },
            title = {
                Text(text = "Create Account")
            },
            text = {
                Column(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.Center
                ){
                    TextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        label = { Text(text = "Display Name") }
                    )
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(text = "Email") }
                    )
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(text = "Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(text = "Confirm Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )

                    // Show error message if any
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Add login option
                    TextButton(
                        onClick = {
                            viewModel.setAuthMode(AuthMode.LOGIN)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Already have an account? Sign in")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(5.dp),
            containerColor = Color.White,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}
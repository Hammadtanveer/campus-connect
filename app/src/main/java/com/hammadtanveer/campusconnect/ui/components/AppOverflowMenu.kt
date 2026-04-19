package com.hammadtanveer.campusconnect.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hammadtanveer.campusconnect.MainViewModel
import com.hammadtanveer.campusconnect.data.models.UserProfile
import com.hammadtanveer.campusconnect.security.PermissionManager
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppOverflowMenu(
    userProfile: UserProfile?,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: MainViewModel = hiltViewModel()
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountPassword by remember { mutableStateOf("") }
    var deleteAccountPasswordVisible by remember { mutableStateOf(false) }
    var deleteAccountLoading by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    val isSuperAdmin = PermissionManager.isSuperAdmin(userProfile)
    val canAccessAdminPanel = PermissionManager.canAccessAdminPanel(userProfile)

    IconButton(onClick = { showOverflowMenu = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
    }

    DropdownMenu(
        expanded = showOverflowMenu,
        onDismissRequest = { showOverflowMenu = false },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = MenuDefaults.TonalElevation
    ) {
        if (canAccessAdminPanel) {
            DropdownMenuItem(
                text = {
                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        text = if (isSuperAdmin) {
                            "Admin Panel (full access)"
                        } else {
                            "Admin Panel"
                        }
                    )
                },
                contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
                onClick = {
                    showOverflowMenu = false
                    onOpenAdminPanel()
                }
            )
            HorizontalDivider()
        }

        DropdownMenuItem(
            text = { Text("About App", style = MaterialTheme.typography.bodyMedium) },
            contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
            onClick = {
                showOverflowMenu = false
                showAboutDialog = true
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    "Delete Account",
                    color = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                showOverflowMenu = false
                showDeleteAccountDialog = true
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )

        DropdownMenuItem(
            text = { Text("Logout", style = MaterialTheme.typography.bodyMedium) },
            contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
            onClick = {
                showOverflowMenu = false
                onLogout()
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About CampusConnect") },
            text = {
                Text(
                    "CampusConnect helps students and admins manage notes, placements, events, societies, and campus updates from one place."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAccountDialog = false
                deleteAccountPassword = ""
                deleteAccountError = null
            },
            title = { Text("Delete Account?") },
            text = {
                Column {
                    Text(
                        "This will permanently delete your account " +
                            "and all your data. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Enter your password to confirm:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteAccountPassword,
                        onValueChange = {
                            deleteAccountPassword = it
                            deleteAccountError = null
                        },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (deleteAccountPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                deleteAccountPasswordVisible = !deleteAccountPasswordVisible
                            }) {
                                Icon(
                                    imageVector = if (deleteAccountPasswordVisible)
                                        Icons.Default.Info
                                    else
                                        Icons.Default.Close,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !deleteAccountLoading
                    )
                    deleteAccountError?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteAccountPassword.isBlank()) {
                            deleteAccountError = "Please enter your password"
                            return@Button
                        }
                        deleteAccountLoading = true
                        deleteAccountError = null
                        viewModel.deleteAccount(deleteAccountPassword) { ok, err ->
                            deleteAccountLoading = false
                            if (ok) {
                                showDeleteAccountDialog = false
                                deleteAccountPassword = ""
                            } else {
                                deleteAccountError = err
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !deleteAccountLoading && deleteAccountPassword.isNotBlank()
                ) {
                    Text(if (deleteAccountLoading) "Deleting..." else "Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    deleteAccountPassword = ""
                    deleteAccountError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}


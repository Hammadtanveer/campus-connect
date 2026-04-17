package com.example.campusconnect.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.security.PermissionManager

@Composable
fun AppOverflowMenu(
    userProfile: UserProfile?,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val isSuperAdmin = PermissionManager.isSuperAdmin(userProfile)
    val canAccessAdminPanel = PermissionManager.canAccessAdminPanel(userProfile)

    IconButton(onClick = { menuExpanded = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
    }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
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
                    menuExpanded = false
                    onOpenAdminPanel()
                }
            )
            HorizontalDivider()
        }

        DropdownMenuItem(
            text = { Text("About App", style = MaterialTheme.typography.bodyMedium) },
            contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
            onClick = {
                menuExpanded = false
                showAboutDialog = true
            }
        )

        DropdownMenuItem(
            text = { Text("Logout", style = MaterialTheme.typography.bodyMedium) },
            contentPadding = MenuDefaults.DropdownMenuItemContentPadding,
            onClick = {
                menuExpanded = false
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
}


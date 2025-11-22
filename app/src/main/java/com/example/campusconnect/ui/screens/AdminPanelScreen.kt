package com.example.campusconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.security.canCreateEvent
import com.example.campusconnect.security.canUploadNotes
import com.example.campusconnect.security.canUpdateSenior
import com.example.campusconnect.security.canManageSociety

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: MainViewModel, navController: NavController) {
    val profile = viewModel.userProfile
    val isAdmin = profile?.isAdmin == true
    val roles = profile?.roles ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdmin) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isAdmin) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isAdmin) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isAdmin) "Admin Access Granted" else "Limited Access",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "User: ${profile?.displayName ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Email: ${profile?.email ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Permissions Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your Permissions",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (roles.isEmpty() && !isAdmin) {
                        Text(
                            text = "No special permissions assigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (isAdmin) {
                            PermissionItem(
                                icon = Icons.Default.Settings,
                                title = "Full Admin Access",
                                description = "All permissions granted"
                            )
                        }

                        if (profile?.canCreateEvent() == true) {
                            PermissionItem(
                                icon = Icons.Default.DateRange,
                                title = "Event Management",
                                description = "Create and manage events"
                            )
                        }

                        if (profile?.canUploadNotes() == true) {
                            PermissionItem(
                                icon = Icons.Default.Star,
                                title = "Notes Upload",
                                description = "Upload and manage study notes"
                            )
                        }

                        if (profile?.canUpdateSenior() == true) {
                            PermissionItem(
                                icon = Icons.Default.Person,
                                title = "Senior Management",
                                description = "Update senior profiles"
                            )
                        }

                        if (profile?.canManageSociety() == true) {
                            PermissionItem(
                                icon = Icons.Default.AccountCircle,
                                title = "Society Management",
                                description = "Manage society pages"
                            )
                        }
                    }
                }
            }

            // Quick Actions Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (profile?.canCreateEvent() == true) {
                        AdminActionButton(
                            icon = Icons.Default.Add,
                            title = "Create Event",
                            onClick = { navController.navigate("events/create") }
                        )
                    }

                    if (profile?.canUploadNotes() == true) {
                        AdminActionButton(
                            icon = Icons.Default.Add,
                            title = "Upload Notes",
                            onClick = {
                                navController.navigate("notes")
                            }
                        )
                    }

                    AdminActionButton(
                        icon = Icons.Default.Refresh,
                        title = "Refresh Permissions",
                        onClick = {
                            viewModel.refreshClaims {
                                // Permissions refreshed
                            }
                        }
                    )
                }
            }

            // Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "How to get admin access?",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Text(
                        text = "Contact your system administrator to assign admin roles. After roles are assigned, sign out and sign back in, or use the 'Refresh Permissions' button above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Role Details (for debugging/info)
            if (roles.isNotEmpty() || isAdmin) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Technical Details",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Text(
                            text = "Admin Flag: $isAdmin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (roles.isNotEmpty()) {
                            Text(
                                text = "Roles: ${roles.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Enabled",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AdminActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Text(title)
        }
    }
}


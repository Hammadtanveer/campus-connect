package com.example.campusconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.models.UserProfile
import com.example.campusconnect.security.PermissionManager
import com.example.campusconnect.ui.viewmodels.AdminPanelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: MainViewModel,
    navController: NavController,
    adminPanelViewModel: AdminPanelViewModel = hiltViewModel()
) {
    val profile by adminPanelViewModel.currentUser.collectAsStateWithLifecycle()

    val isSuperAdmin = PermissionManager.isSuperAdmin(profile)
    val isAdmin = PermissionManager.canAccessAdminPanel(profile)
    val featureVisibility = adminPanelViewModel.getFeatureVisibility(profile)

    LaunchedEffect(profile, isAdmin, isSuperAdmin, featureVisibility) {
        val perms = PermissionManager.effectivePermissions(profile).sorted()
        android.util.Log.d("ADMIN_DEBUG", "UI permissions = ${profile?.permissions}")
        android.util.Log.d(
            "PERM_DEBUG",
            "UI AdminPanelScreen -> role=${profile?.role ?: ""}, perms=$perms, isAdmin=$isAdmin, isSuperAdmin=$isSuperAdmin, features=$featureVisibility"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isSuperAdmin) "Admin Panel (Super Admin)" else "Admin Panel") },
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
            AdminIdentityCard(profile = profile, isAdmin = isAdmin, isSuperAdmin = isSuperAdmin)

            if (!isAdmin && !featureVisibility.contentModeration) {
                AccessDeniedCard()
                return@Column
            }

            FeatureCardsSection(
                featureVisibility = featureVisibility,
                onOpenUserManagement = { navController.navigate("admin/users") },
                onOpenAnalytics = { navController.navigate("admin/analytics") },
                onOpenContentModeration = { navController.navigate("admin/content-moderation") },
                onOpenSendNotification = { navController.navigate("admin/send-notification") },
                onOpenActivityLog = { navController.navigate("admin/activity-log") }
            )
        }
    }
}

@Composable
private fun AdminIdentityCard(profile: UserProfile?, isAdmin: Boolean, isSuperAdmin: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (isAdmin) Icons.Default.Settings else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when {
                        isSuperAdmin -> "Super Admin Access"
                        isAdmin -> "Admin Access"
                        else -> "Student Access"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Text("User: ${profile?.displayName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Email: ${profile?.email ?: "Unknown"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureCardsSection(
    featureVisibility: AdminPanelViewModel.AdminFeatureVisibility,
    onOpenUserManagement: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenContentModeration: () -> Unit,
    onOpenSendNotification: () -> Unit,
    onOpenActivityLog: () -> Unit
) {
    if (featureVisibility.analytics) {
        AdminFeatureCard(
            title = "Analytics",
            description = "Track usage trends and admin insights.",
            actionLabel = "Open",
            onClick = onOpenAnalytics
        )
    }

    if (featureVisibility.userManagement) {
        AdminFeatureCard(
            title = "User Management + Permission Assignment",
            description = "Search users and manage role permissions.",
            actionLabel = "Open",
            onClick = onOpenUserManagement
        )
    }

    if (featureVisibility.contentModeration) {
        AdminFeatureCard(
            title = "Content Moderation",
            description = "Review and manage notes/events content.",
            actionLabel = "Open",
            onClick = onOpenContentModeration
        )
    }

    if (featureVisibility.sendNotification) {
        AdminFeatureCard(
            title = "Send Notification",
            description = "Broadcast announcements to users.",
            actionLabel = "Open",
            onClick = onOpenSendNotification
        )
    }


    if (featureVisibility.activityLog) {
        AdminFeatureCard(
            title = "Activity Log",
            description = "View admin actions and audit history.",
            actionLabel = "Open",
            onClick = onOpenActivityLog
        )
    }
}

@Composable
private fun AdminFeatureCard(
    title: String,
    description: String,
    actionLabel: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AssistChip(onClick = { onClick?.invoke() }, label = { Text(actionLabel) })
        }
    }
}


@Composable
private fun AccessDeniedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "No admin permissions assigned",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                "Contact super admin to assign management permissions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}


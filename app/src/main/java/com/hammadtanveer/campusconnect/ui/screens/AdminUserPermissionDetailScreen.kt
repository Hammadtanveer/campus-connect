package com.hammadtanveer.campusconnect.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.ui.viewmodels.AdminPanelViewModel

private val PermissionLabels = mapOf(
    "placements:manage" to "Manage Placements",
    "notes:manage" to "Manage Notes",
    "meetings:manage" to "Manage Meetings & Announcements",
    "seniors:manage" to "Manage Seniors",
    "society:csss:manage" to "Manage Society: CSSS",
    "society:hobbies_club:manage" to "Manage Society: Hobbies Club",
    "society:tech_club:manage" to "Manage Society: Tech Club",
    "society:sports_club:manage" to "Manage Society: Sports Club",
    "society:cultural_society:manage" to "Manage Society: Cultural Society",
    "society:literary_society:manage" to "Manage Society: Literary Society"
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminUserPermissionDetailScreen(
    navController: NavController,
    userId: String,
    viewModel: AdminPanelViewModel = hiltViewModel()
) {
    val selectedUserState by viewModel.selectedUserState.collectAsStateWithLifecycle()
    val updateStatus by viewModel.permissionUpdateStatus.collectAsStateWithLifecycle()
    val deleteUserStatus by viewModel.deleteUserStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        viewModel.loadUserById(userId)
    }

    DisposableEffect(userId) {
        onDispose { viewModel.stopObservingSelectedUser() }
    }

    val targetUser = (selectedUserState as? Resource.Success)?.data
    val editablePermissions = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(targetUser?.id, targetUser?.permissions) {
        editablePermissions.clear()
        val permissions = targetUser
            ?.permissions
            ?.map { PermissionManager.normalizePermission(it) }
            ?.toSet()
            ?: emptySet()

        fun hasPermission(key: String): Boolean {
            return permissions.contains(PermissionManager.normalizePermission(key))
        }

        AdminPanelViewModel.MANAGED_PERMISSION_KEYS.forEach { key ->
            editablePermissions[key] = hasPermission(key)
        }
        Log.d("ADMIN_FINAL", "permissions=${targetUser?.permissions}")
    }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(updateStatus) {
        when (val status = updateStatus) {
            is Resource.Success -> {
                isSaving = false
                Toast.makeText(context, "Permissions updated", Toast.LENGTH_SHORT).show()
                viewModel.resetPermissionUpdateStatus()
                navController.navigateUp()
            }
            is Resource.Error -> {
                isSaving = false
                Toast.makeText(context, status.message ?: "Permission update failed", Toast.LENGTH_SHORT).show()
                viewModel.resetPermissionUpdateStatus()
            }
            is Resource.Loading -> {
                isSaving = true
            }
            null -> Unit
        }
    }

    LaunchedEffect(deleteUserStatus) {
        when (val status = deleteUserStatus) {
            is Resource.Success -> {
                Toast.makeText(context, "User removed", Toast.LENGTH_SHORT).show()
                viewModel.resetDeleteUserStatus()
                navController.popBackStack("admin/users", inclusive = false)
            }
            is Resource.Error -> {
                Toast.makeText(context, status.message ?: "Failed to remove user", Toast.LENGTH_SHORT).show()
                viewModel.resetDeleteUserStatus()
            }
            is Resource.Loading, null -> Unit
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove User") },
            text = { Text("Are you sure you want to remove this user?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteUser(userId)
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Permission Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when (val state = selectedUserState) {
            null,
            is Resource.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message ?: "Failed to load user",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is Resource.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = targetUser?.displayName?.ifBlank { "Unnamed user" } ?: "Unnamed user",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = targetUser?.email.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AdminPanelViewModel.MANAGED_PERMISSION_KEYS.forEach { key ->
                        PermissionToggleRow(
                            title = PermissionLabels[key] ?: key,
                            enabled = editablePermissions[key] == true,
                            onChanged = { editablePermissions[key] = it }
                        )
                    }

                    Button(
                        onClick = { viewModel.updatePermissionsForUser(userId, editablePermissions.toMap()) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text("Saving...")
                        } else {
                            Text("Save Permissions")
                        }
                    }

                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Remove User")
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    enabled: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = enabled, onCheckedChange = onChanged)
        }
    }
}






package com.example.campusconnect.ui.screens
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.ui.placement.PlacementViewModel
import com.example.campusconnect.MainViewModel

@Composable
fun PlacementCareerScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    val placementsState by viewModel.placements.collectAsState()
    val deleteStatus by viewModel.deletePlacementStatus.collectAsState()
    val userProfile = mainViewModel.userProfile
    val context = LocalContext.current

    val currentRole = userProfile?.role
    val canDeleteJobs = (userProfile?.isAdmin == true) ||
        currentRole.equals("admin", ignoreCase = true) ||
        currentRole.equals("super_admin", ignoreCase = true) ||
        currentRole.equals("superadmin", ignoreCase = true)
    val canManageJobs = canDeleteJobs

    var pendingDelete by remember { mutableStateOf<Placement?>(null) }

    LaunchedEffect(deleteStatus) {
        when (val status = deleteStatus) {
            is Resource.Success<*> -> {
                Toast.makeText(context, "Job deleted", Toast.LENGTH_SHORT).show()
                viewModel.resetDeletePlacementStatus()
            }
            is Resource.Error -> {
                Toast.makeText(context, status.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
                viewModel.resetDeletePlacementStatus()
            }
            is Resource.Loading, null -> Unit
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete Job") },
            text = { Text("Are you sure you want to delete this job?") },
            confirmButton = {
                TextButton(onClick = {
                    val target = pendingDelete
                    if (target != null) {
                        viewModel.deletePlacement(
                            id = target.id,
                            currentUserRole = currentRole,
                            isAdminFlag = userProfile?.isAdmin == true
                        )
                    }
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Placements", style = MaterialTheme.typography.headlineMedium)

            if (canManageJobs) {
                Button(onClick = { navController.navigate("placement/add") }) {
                    Text("Post Job")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (val state = placementsState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Text(text = "Error: ", color = MaterialTheme.colorScheme.error)
            }
            is Resource.Success -> {
                 if (state.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No placements",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No placements posted yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.data, key = { it.id }) { placement ->
                            PlacementListItem(
                                placement = placement,
                                onClick = { navController.navigate("placement/${placement.id}") },
                                canEdit = canManageJobs,
                                canDelete = canDeleteJobs,
                                onEditClick = { navController.navigate("placement/edit/${placement.id}") },
                                onDeleteClick = { pendingDelete = placement }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PlacementListItem(
    placement: Placement,
    onClick: () -> Unit,
    canEdit: Boolean,
    canDelete: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val normalizedSalary = placement.salary.trim().let { rawSalary ->
        when {
            rawSalary.isBlank() -> "Salary N/A"
            rawSalary.contains("lpa", ignoreCase = true) -> rawSalary
            else -> "$rawSalary LPA"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = placement.role.ifBlank { "Role TBA" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = normalizedSalary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    if (canEdit) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Job",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Job",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
             Spacer(modifier = Modifier.height(4.dp))
            Text(text = placement.companyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = placement.description, 
                maxLines = 3, 
                overflow = TextOverflow.Ellipsis, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
             Spacer(modifier = Modifier.height(12.dp))
             Button(
                 onClick = {
                     if (placement.applyLink.isNotBlank()) {
                         try {
                             val intent = Intent(Intent.ACTION_VIEW, Uri.parse(placement.applyLink))
                             context.startActivity(intent)
                         } catch (e: Exception) {
                             // Ignore
                         }
                     }
                 },
                 modifier = Modifier.align(Alignment.End),
                 enabled = placement.applyLink.isNotBlank()
             ) {
                 Text("Apply Now")
             }
        }
    }
}

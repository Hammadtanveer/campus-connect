package com.hammadtanveer.campusconnect.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.ui.viewmodels.EventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocietyEventDetailScreen(
    societyId: String,
    eventId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.currentUserProfileFlow.collectAsStateWithLifecycle(null)

    LaunchedEffect(eventId, societyId) {
        viewModel.getSocietyEvent(societyId, eventId)
    }

    val event = viewModel.selectedEvent
    val isLoading = viewModel.isLoading
    val deleteStatus = viewModel.deleteEventStatus

    LaunchedEffect(deleteStatus) {
        when (deleteStatus) {
            is Resource.Success -> {
                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
                onBack()
            }
            is Resource.Error -> Toast.makeText(context, deleteStatus.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
            is Resource.Loading, null -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (event != null && viewModel.canEditSocietyEvent(profile, societyId)) {
                        IconButton(onClick = { onEdit(event.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    if (event != null && viewModel.canDeleteSocietyEvent(profile, societyId)) {
                        IconButton(onClick = {
                            viewModel.deleteEvent(societyId, event.id, profile)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets
                    .only(WindowInsetsSides.Horizontal)
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (event != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 0.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "SOCIETY EVENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = event.societyName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Organized by ${event.societyName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            SocietyEventDetailRow(
                                icon = Icons.Default.CalendarMonth,
                                text = "${event.date} at ${event.time}"
                            )
                            SocietyEventDetailRow(
                                icon = Icons.Default.LocationOn,
                                text = event.venue.ifBlank { "Venue TBA" }
                            )
                            
                            if (event.coordinator.isNotBlank()) {
                                SocietyEventDetailRow(
                                    icon = Icons.Default.Person,
                                    text = "Coordinator: ${event.coordinator}"
                                )
                            }
                            
                            if (event.convener.isNotBlank()) {
                                SocietyEventDetailRow(
                                    icon = Icons.Default.SupervisorAccount,
                                    text = "Convener: ${event.convener}"
                                )
                            }

                            if (event.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (event.posterUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                AsyncImage(
                                    model = event.posterUrl,
                                    contentDescription = "Event Poster",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }

                            if (event.register.isNotBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        runCatching {
                                            val registrationUri = event.register.trim().toUri()
                                            val intent = Intent(Intent.ACTION_VIEW, registrationUri)
                                            context.startActivity(intent)
                                        }.onFailure {
                                            Toast.makeText(context, "Invalid registration link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Register Now")
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Text("Event not found", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun SocietyEventDetailRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

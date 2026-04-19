package com.hammadtanveer.campusconnect.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text(event?.name ?: "Event Details") },
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
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (event != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (event.posterUrl.isNotBlank()) {
                        AsyncImage(
                            model = event.posterUrl,
                            contentDescription = "Event Poster",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    DetailItem(label = "Event Name", value = event.name)
                    DetailItem(label = "Date", value = event.date)
                    DetailItem(label = "Time", value = event.time)
                    DetailItem(label = "Venue", value = event.venue)
                    DetailItem(label = "Student Coordinator", value = event.coordinator)
                    DetailItem(label = "Faculty Convener", value = event.convener)
                    DetailItem(label = "Registration Link", value = event.register)

                    Button(
                        onClick = {
                            runCatching {
                                val registrationUri = event.register.trim().toUri()
                                val intent = Intent(Intent.ACTION_VIEW, registrationUri)
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "No app found to open registration link", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure {
                                Toast.makeText(context, "Invalid registration link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = event.register.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Now")
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
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

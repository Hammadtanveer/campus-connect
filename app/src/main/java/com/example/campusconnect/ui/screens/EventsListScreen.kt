package com.example.campusconnect.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.models.OnlineEvent
import com.example.campusconnect.data.models.Resource

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.ui.events.EventsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun EventsListScreen(
    navController: NavController,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val resource by viewModel.eventsState.collectAsState()

    val eventsList = remember { mutableStateOf<List<OnlineEvent>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resource) {
        when (val res = resource) {
            is Resource.Loading -> {
                isLoading.value = true
                error.value = null
            }
            is Resource.Success -> {
                isLoading.value = false
                eventsList.value = res.data
                error.value = null
            }
            is Resource.Error -> {
                isLoading.value = false
                error.value = res.message
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Events", style = MaterialTheme.typography.headlineMedium)
            if (viewModel.canCreateEvent()) {
                Button(onClick = { navController.navigate("events/create") }) { Text("Create Event") }
            } else {
                AssistChip(onClick = {}, label = { Text("View only") })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading.value) {
            Text(text = "Loading events...", style = MaterialTheme.typography.bodyMedium)
        }

        error.value?.let { err ->
            Text(text = "Error: $err", color = MaterialTheme.colorScheme.error)
        }

        if (!isLoading.value && eventsList.value.isEmpty() && error.value == null) {
            // Empty state similar to downloads
            Text("No events available.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        LazyColumn {
            items(eventsList.value) { event ->
                EventListItem(
                    event = event,
                    onClick = { navController.navigate("event/${event.id}") },
                    canEdit = viewModel.canEditEvent(event),
                    canDelete = viewModel.canDeleteEvent(event),
                    onEdit = { /* TODO: Navigate to Edit Event */ },
                    onDelete = {
                        viewModel.deleteEvent(event.id) { success, _ ->
                            if (!success) {
                                // Handle error
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EventListItem(
    event: OnlineEvent,
    onClick: () -> Unit,
    canEdit: Boolean = false,
    canDelete: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val start = event.dateTime?.toDate()?.time ?: 0L
    val end = start + (event.durationMinutes * 60_000L)
    val isHappeningNow = event.meetLink.isNotBlank() && now in (start..end)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.dateTime?.toDate()?.toString()?.take(16) ?: "Date TBA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By ${event.organizerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Event", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Event", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (isHappeningNow) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Handle error
                        }
                    }
                ) {
                    Text("Join")
                }
            } else {
                AssistChip(
                    onClick = { onClick() },
                    label = {
                        Text(
                            text = if (event.participantCount > 0) "${event.participantCount} Joined" else "View",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

fun cleanUpImports() {
    // This is a dummy edit to force re-evaluation of the file by the compiler/IDE state
}

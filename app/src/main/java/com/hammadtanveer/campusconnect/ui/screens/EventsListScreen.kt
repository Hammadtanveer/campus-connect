package com.hammadtanveer.campusconnect.ui.screens

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
import com.hammadtanveer.campusconnect.data.models.OnlineEvent
import com.hammadtanveer.campusconnect.data.models.Resource

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hammadtanveer.campusconnect.security.PermissionManager
import com.hammadtanveer.campusconnect.ui.events.EventsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.hammadtanveer.campusconnect.data.models.EventType

@Composable
fun EventsListScreen(
    navController: NavController,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val resource by viewModel.eventsState.collectAsStateWithLifecycle()
    val profile by viewModel.currentUserProfileFlow.collectAsStateWithLifecycle(null)

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
            Text(
                text = "Meetings & Announcements",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (PermissionManager.canCreateEvents(profile)) {
                Button(onClick = { navController.navigate("events/create") }) { Text("Create") }
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
                    canEdit = viewModel.canEditEvent(event, profile),
                    canDelete = viewModel.canDeleteEvent(event, profile),
                    onEdit = { navController.navigate("events/edit/${event.id}") },
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
    val end = start + (event.duration * 60_000L)
    val isHappeningNow = event.meetLink.isNotBlank() && now in (start..end)
    val isFull = event.maxParticipants > 0 && event.participantCount >= event.maxParticipants

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val categoryColor = when (event.eventType) {
                            EventType.ONLINE -> MaterialTheme.colorScheme.tertiary
                            EventType.OFFLINE -> MaterialTheme.colorScheme.secondary
                        }
                        Text(
                            text = event.eventType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier
                                .background(categoryColor.copy(alpha = 0.1f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        if (isHappeningNow) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (canEdit || canDelete) {
                    Row {
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Event Details Grid-like layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EventInfoItem(
                    icon = Icons.Default.Schedule,
                    text = event.dateTime?.toDate()?.toString()?.take(16) ?: "Date TBA",
                    modifier = Modifier.weight(1f)
                )
                EventInfoItem(
                    icon = Icons.Filled.Person,
                    text = if (event.maxParticipants > 0) "${event.participantCount}/${event.maxParticipants}" else "${event.participantCount} Joined",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EventInfoItem(
                    icon = if (event.eventType == EventType.ONLINE) Icons.Filled.Videocam else Icons.Filled.LocationOn,
                    text = if (event.eventType == EventType.ONLINE) "Online Meet" else event.venue.ifBlank { "Venue TBA" },
                    modifier = Modifier.weight(1f)
                )
                EventInfoItem(
                    icon = Icons.Default.Schedule,
                    text = "${event.duration} mins",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${event.organizerName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isHappeningNow) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Handle error
                            }
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Join Now")
                    }
                } else if (isFull) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Full") },
                        enabled = false
                    )
                } else {
                    Button(
                        onClick = onClick,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("View Details")
                    }
                }
            }
        }
    }
}

@Composable
fun EventInfoItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


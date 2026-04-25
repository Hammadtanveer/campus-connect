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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hammadtanveer.campusconnect.MainViewModel
import com.hammadtanveer.campusconnect.data.models.EventType
import com.hammadtanveer.campusconnect.data.models.OnlineEvent
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.ui.events.EventsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String?,
    navController: NavController,
    mainViewModel: MainViewModel,
    eventsViewModel: EventsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val eventState = remember { mutableStateOf<OnlineEvent?>(null) }
    val loading = remember { mutableStateOf(true) }

    LaunchedEffect(eventId) {
        if (eventId == null) return@LaunchedEffect

        loading.value = true
        val result = eventsViewModel.getEvent(eventId)
        if (result is Resource.Success) {
            eventState.value = result.data
        }
        loading.value = false
    }

    val event = eventState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (loading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (event == null) {
                Text("Event not found", modifier = Modifier.align(Alignment.Center))
            } else {
                val dateFormatter = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
                val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

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
                                            imageVector = if (event.eventType == EventType.ONLINE) Icons.Default.Videocam else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = event.eventType.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = event.category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Created by ${event.organizerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                            EventDetailRow(
                                icon = Icons.Default.CalendarMonth,
                                text = event.dateTime?.toDate()?.let { dateFormatter.format(it) } ?: "Date TBA"
                            )
                            EventDetailRow(
                                icon = Icons.Default.Schedule,
                                text = event.dateTime?.toDate()?.let { timeFormatter.format(it) } ?: "Time TBA"
                            )
                            EventDetailRow(
                                icon = Icons.Default.Timer,
                                text = "${event.duration} minutes"
                            )

                            if (event.eventType == EventType.OFFLINE) {
                                EventDetailRow(
                                    icon = Icons.Default.Place,
                                    text = event.venue.ifBlank { "Venue TBA" }
                                )
                            } else {
                                EventDetailRow(
                                    icon = Icons.Default.Link,
                                    text = event.meetLink.ifBlank { "Link TBA" }
                                )
                            }

                            if (event.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (event.eventType == EventType.ONLINE && event.meetLink.isNotBlank()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Join Meeting")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailRow(icon: ImageVector, text: String) {
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

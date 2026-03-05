package com.example.campusconnect.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.SocietyEvent
import com.example.campusconnect.ui.viewmodels.EventViewModel

private data class SocietyUi(val id: String, val name: String)

@Composable
fun Societies(navController: NavController) {
    val societies = listOf(
        SocietyUi("csss", "CSSS"),
        SocietyUi("hobbies_club", "Hobbies Club"),
        SocietyUi("tech_club", "Tech Club"),
        SocietyUi("sports_club", "Sports Club"),
        SocietyUi("cultural_society", "Cultural Society"),
        SocietyUi("literary_society", "Literary Society")
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Societies & Clubs",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(societies) { society ->
                    SocietyCard(
                        name = society.name,
                        onClick = {
                            navController.navigate(
                                "societyEvents/${society.id}/${Uri.encode(society.name)}"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SocietyEventsScreen(
    societyId: String,
    societyName: String,
    navController: NavController,
    viewModel: EventViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(societyId) {
        viewModel.startObservingSocietyEvents(societyId)
    }

    DisposableEffect(societyId) {
        onDispose {
            viewModel.stopObservingSocietyEvents(societyId)
        }
    }

    val state by viewModel.societyEventsState.collectAsState()
    val deleteStatus = viewModel.deleteEventStatus

    val canCreate = remember(viewModel.currentUserRole, viewModel.isRoleLoading) { viewModel.canCreateSocietyEvent() }
    val canEdit = remember(viewModel.currentUserRole, viewModel.isRoleLoading) { viewModel.canEditSocietyEvent() }
    val canDelete = remember(viewModel.currentUserRole, viewModel.isRoleLoading) { viewModel.canDeleteSocietyEvent() }

    LaunchedEffect(deleteStatus) {
        when (deleteStatus) {
            is Resource.Success -> {
                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
            }
            is Resource.Error -> Toast.makeText(context, deleteStatus.message ?: "Delete failed", Toast.LENGTH_SHORT).show()
            is Resource.Loading, null -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(societyName, style = MaterialTheme.typography.headlineSmall)
            }

            if (canCreate) {
                Button(onClick = {
                    navController.navigate("societyEvent/create/$societyId/${Uri.encode(societyName)}")
                }) {
                    Text("Create")
                }
            } else {
                AssistChip(onClick = {}, label = { Text("View only") })
            }
        }

        when (val resource = state) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(resource.message ?: "Failed to load events", color = MaterialTheme.colorScheme.error)
                }
            }

            is Resource.Success -> {
                if (resource.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No events posted for $societyName yet.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            items = resource.data,
                            key = { it.id },
                            contentType = { "society_event_card" }
                        ) { event ->
                            SocietyEventCard(
                                event = event,
                                canEdit = canEdit,
                                canDelete = canDelete,
                                onOpen = { navController.navigate("societyEvent/$societyId/${event.id}") },
                                onEdit = { navController.navigate("societyEvent/edit/$societyId/${event.id}") },
                                onDelete = { viewModel.deleteEvent(societyId, event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocietyEventCard(
    event: SocietyEvent,
    canEdit: Boolean,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (event.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = event.posterUrl,
                    contentDescription = "Event Poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
            }

            Text(
                text = event.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text("Date: ${event.date}")
            Text("Time: ${event.time}")
            Text("Venue: ${event.venue}")
            Text("Student Coordinator: ${event.coordinator}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Faculty Convener: ${event.convener}", maxLines = 1, overflow = TextOverflow.Ellipsis)

            if (canEdit || canDelete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocietyCard(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SocietiesPreview() {
    Societies(rememberNavController())
}

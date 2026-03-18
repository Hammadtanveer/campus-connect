package com.example.campusconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.ui.viewmodels.ContentModerationViewModel

private enum class ModerationFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminContentModerationScreen(
    navController: NavController,
    viewModel: ContentModerationViewModel = hiltViewModel()
) {
    val notesState by viewModel.notesState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val updatingNoteIds by viewModel.updatingNoteIds.collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(ModerationFilter.PENDING) }

    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is Resource.Success -> {
                Toast.makeText(context, "Moderation updated", Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }
            is Resource.Error -> {
                Toast.makeText(context, state.message ?: "Update failed", Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }
            is Resource.Loading, null -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Content Moderation") },
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
        when (val state = notesState) {
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
                        text = state.message ?: "Unable to load notes",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is Resource.Success -> {
                val filteredNotes = when (selectedFilter) {
                    ModerationFilter.ALL -> state.data
                    ModerationFilter.PENDING -> state.data.filter {
                        it.moderationStatus.equals("pending", ignoreCase = true) || it.moderationStatus.isBlank()
                    }
                    ModerationFilter.APPROVED -> state.data.filter { it.moderationStatus.equals("approved", ignoreCase = true) }
                    ModerationFilter.REJECTED -> state.data.filter { it.moderationStatus.equals("rejected", ignoreCase = true) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    TabRow(selectedTabIndex = selectedFilter.ordinal) {
                        ModerationFilter.entries.forEach { filter ->
                            Tab(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                text = { Text(filter.label) }
                            )
                        }
                    }

                    if (filteredNotes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No notes in ${selectedFilter.label.lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                ModerationNoteCard(
                                    note = note,
                                    isUpdating = updatingNoteIds.contains(note.id),
                                    onApprove = { viewModel.approveNote(note.id) },
                                    onReject = { viewModel.rejectNote(note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationNoteCard(
    note: Note,
    isUpdating: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${note.subject} • ${note.semester}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Uploader: ${note.uploaderName.ifBlank { note.uploaderId }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    OutlinedButton(onClick = onReject) { Text("Reject") }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}


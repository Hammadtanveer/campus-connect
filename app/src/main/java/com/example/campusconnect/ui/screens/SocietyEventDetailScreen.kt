package com.example.campusconnect.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.data.models.SocietyEvent
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.ui.viewmodels.EventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocietyEventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(eventId) {
        viewModel.getSocietyEvent(eventId)
        // You'll need to implement getSocietyEvent in your ViewModel
        // For now, let's assume it exists or we fetch it from Firestore
        // viewModel.getSocietyEvent(eventId).collect { ... }
    }
//    var event by remember { mutableStateOf<SocietyEvent?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val event = viewModel.selectedEvent
    val isLoading = viewModel.loding

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.name ?: "Event Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Placeholder implementation until ViewModel fetch is ready
        // In a real scenario, you'd fetch this from Firestore using eventId
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (event != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailItem(label = "Event Name", value = event.name)
                    DetailItem(label = "Date", value = event.date)
                    DetailItem(label = "Time", value = event.time)
                    DetailItem(label = "Venue", value = event.venue)
                    DetailItem(label = "Coordinator", value = event.coordinator)
                    DetailItem(label = "Convener", value = event.convener)


                    if (event.register.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.register))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Register Now")
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
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

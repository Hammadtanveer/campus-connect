package com.example.campusconnect.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.models.OnlineEvent
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.util.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EventsListScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val eventsState = remember { mutableStateOf<List<OnlineEvent>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadEvents().collect { res ->
            when (res) {
                is Resource.Loading -> {
                    isLoading.value = true
                }
                is Resource.Success -> {
                    isLoading.value = false
                    eventsState.value = res.data
                }
                is Resource.Error -> {
                    isLoading.value = false
                    error.value = res.message
                }
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
            Button(onClick = { navController.navigate("events/create") }) {
                Text("Create Event")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading.value) {
            Text(text = "Loading events...", style = MaterialTheme.typography.bodyMedium)
        }

        error.value?.let { err ->
            Text(text = "Error: $err", color = MaterialTheme.colorScheme.error)
        }

        if (!isLoading.value && eventsState.value.isEmpty()) {
            // Empty state similar to downloads
            Text("No events available.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        LazyColumn {
            items(eventsState.value) { event ->
                EventListItem(event = event, onClick = { navController.navigate("event/${event.id}") }, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EventListItem(event: OnlineEvent, onClick: () -> Unit, viewModel: MainViewModel) {
    val participantCount = remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    LaunchedEffect(event.id) {
        val db = FirebaseFirestore.getInstance()
        db.collection("registrations").whereEqualTo("eventId", event.id).get()
            .addOnSuccessListener { snap -> participantCount.value = snap.size() }
            .addOnFailureListener { participantCount.value = null }
    }

    val now = System.currentTimeMillis()
    val start = event.dateTime?.toDate()?.time ?: 0L
    val end = start + (event.durationMinutes * 60_000L)
    val joinNow = event.meetLink.isNotBlank() && now in (start..end)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = event.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                participantCount.value?.let { count ->
                    Text(text = "Participants: $count/${if (event.maxParticipants <= 0) "—" else event.maxParticipants}", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                if (joinNow) {
                    Button(onClick = {
                        if (!NetworkUtils.isNetworkAvailable(context)) return@Button
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.meetLink))
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val browser = Intent(Intent.ACTION_VIEW, Uri.parse(event.meetLink))
                            browser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(browser)
                        }
                    }) {
                        Text("Join Now")
                    }
                } else {
                    Button(onClick = { onClick() }) {
                        Text("View")
                    }
                }
            }
        }
    }
}

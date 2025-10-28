package com.example.campusconnect.ui.theme

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.OnlineEvent
import com.example.campusconnect.Resource
import kotlinx.coroutines.flow.collect

@Composable
fun EventsListScreen(viewModel: MainViewModel, navController: NavController) {
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

        LazyColumn {
            items(eventsState.value) { event ->
                EventListItem(event = event, onClick = { navController.navigate("event/${event.id}") })
            }
        }
    }
}

@Composable
fun EventListItem(event: OnlineEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                Text(text = event.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.example.campusconnect.R.drawable.baseline_event_24),
                contentDescription = "event",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}


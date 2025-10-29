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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.Resource
import com.example.campusconnect.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorsListScreen(viewModel: MainViewModel, navController: NavController) {
    val mentorsState = remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }

    // Search and filter state
    var query by remember { mutableStateOf("") }
    val selectedExpertise = remember { mutableStateOf<String?>(null) }

    // collect my sent requests and connections
    val sentRequestsState = remember { mutableStateOf<List<String>>(emptyList()) } // receiverIds with pending or accepted
    val connectionsState = remember { mutableStateOf<List<String>>(emptyList()) } // connected user ids

    LaunchedEffect(Unit) {
        viewModel.loadMentors().collect { res ->
            when (res) {
                is Resource.Loading -> isLoading.value = true
                is Resource.Success -> {
                    isLoading.value = false
                    mentorsState.value = res.data
                }
                is Resource.Error -> {
                    isLoading.value = false
                    error.value = res.message
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getMyMentorshipRequests().collect { res ->
            when (res) {
                is Resource.Success -> {
                    val ids = res.data.mapNotNull { it.receiverId }
                    sentRequestsState.value = ids
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getMyConnections().collect { res ->
            when (res) {
                is Resource.Success -> {
                    connectionsState.value = res.data.map { it.id }
                }
                else -> {}
            }
        }
    }

    // derive expertise tags
    val allExpertise = mentorsState.value.flatMap { it.expertise }.distinct().sorted()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Find Mentors", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search by name or expertise") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        // expertise filter chips
        if (allExpertise.isEmpty()) {
            Text(text = "No expertise filters available", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // show a chip for 'All'
                FilterChip(selected = selectedExpertise.value == null, onClick = { selectedExpertise.value = null }, label = { Text("All") })
                allExpertise.forEach { tag ->
                    FilterChip(selected = selectedExpertise.value == tag, onClick = { selectedExpertise.value = if (selectedExpertise.value == tag) null else tag }, label = { Text(tag) })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading.value) {
            Text(text = "Loading mentors...", style = MaterialTheme.typography.bodyMedium)
        }

        error.value?.let { err ->
            Text(text = "Error: $err", color = MaterialTheme.colorScheme.error)
        }

        val filtered = mentorsState.value.filter { mentor ->
            val matchesQuery = query.isBlank() || mentor.displayName.contains(query, ignoreCase = true) || mentor.expertise.any { it.contains(query, ignoreCase = true) }
            val matchesTag = selectedExpertise.value == null || mentor.expertise.contains(selectedExpertise.value)
            matchesQuery && matchesTag
        }

        if (!isLoading.value && filtered.isEmpty()) {
            Text("No mentors match your search.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        }

        LazyColumn {
            items(filtered) { mentor ->
                val alreadyRequested = sentRequestsState.value.contains(mentor.id)
                val connected = connectionsState.value.contains(mentor.id)
                MentorListItem(mentor = mentor, onClick = { navController.navigate("mentor/${mentor.id}") }, requested = alreadyRequested, connected = connected)
            }
        }
    }
}

@Composable
fun MentorListItem(mentor: UserProfile, onClick: () -> Unit, requested: Boolean = false, connected: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = mentor.displayName.ifBlank { "Unnamed" }, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(text = (mentor.expertise.joinToString(", ") { it }), style = MaterialTheme.typography.bodySmall)
                }
                when {
                    connected -> Text(text = "Connected", style = MaterialTheme.typography.labelSmall)
                    requested -> Text(text = "Requested", style = MaterialTheme.typography.labelSmall)
                    else -> Text(text = mentor.mentorshipStatus.capitalize(), style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mentor.mentorshipBio.takeIf { it.isNotBlank() } ?: mentor.bio.takeIf { it.isNotBlank() } ?: "No bio provided.", style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}

package com.example.campusconnect.ui.theme

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.MentorshipRequest
import com.example.campusconnect.Resource
import kotlinx.coroutines.launch

@Composable
fun MyMentorshipScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val received = remember { mutableStateOf<List<MentorshipRequest>>(emptyList()) }
    val sent = remember { mutableStateOf<List<MentorshipRequest>>(emptyList()) }
    val connections = remember { mutableStateOf<List<com.example.campusconnect.UserProfile>>(emptyList()) }

    val loadingReceived = remember { mutableStateOf(false) }
    val loadingSent = remember { mutableStateOf(false) }
    val loadingConnections = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // dialog state
    val showConfirm = remember { mutableStateOf(false) }
    val confirmAction = remember { mutableStateOf("") } // "accept" | "reject" | "remove"
    val targetRequestId = remember { mutableStateOf<String?>(null) }
    val targetConnectionId = remember { mutableStateOf<String?>(null) }

    // track previous pending count to show notification when new arrives
    var previousPending = remember { 0 }

    LaunchedEffect(Unit) {
        viewModel.getReceivedRequests().collect { res ->
            when (res) {
                is Resource.Loading -> loadingReceived.value = true
                is Resource.Success -> {
                    loadingReceived.value = false
                    received.value = res.data
                    val pending = res.data.count { it.status == "pending" }
                    if (pending > previousPending) {
                        viewModel.notifyMentorship(context, "New mentorship request", "You have $pending pending request(s)")
                    }
                    previousPending = pending
                }
                is Resource.Error -> {
                    loadingReceived.value = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getMyMentorshipRequests().collect { res ->
            when (res) {
                is Resource.Loading -> loadingSent.value = true
                is Resource.Success -> {
                    loadingSent.value = false
                    sent.value = res.data
                }
                is Resource.Error -> loadingSent.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getMyConnections().collect { res ->
            when (res) {
                is Resource.Loading -> loadingConnections.value = true
                is Resource.Success -> {
                    loadingConnections.value = false
                    connections.value = res.data
                }
                is Resource.Error -> loadingConnections.value = false
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Mentorship", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Received (for mentors)
        Text("Received Requests", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        if (loadingReceived.value) Text("Loading...", style = MaterialTheme.typography.bodyMedium)
        if (!loadingReceived.value && received.value.isEmpty()) Text("No received requests", style = MaterialTheme.typography.bodyMedium)
        LazyColumn {
            items(received.value) { req ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { navController.navigate("mentorship/request/${req.id}") }, elevation = CardDefaults.cardElevation(4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "From: ${req.senderId}", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Message: ${req.message.takeIf { it.isNotBlank() } ?: "—"}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Status: ${req.status}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row {
                            if (req.status == "pending") {
                                Button(onClick = {
                                    targetRequestId.value = req.id
                                    confirmAction.value = "accept"
                                    showConfirm.value = true
                                }) { Text("Accept") }
                                Spacer(modifier = Modifier.size(8.dp))
                                Button(onClick = {
                                    targetRequestId.value = req.id
                                    confirmAction.value = "reject"
                                    showConfirm.value = true
                                }) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sent
        Text("My Sent Requests", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        if (loadingSent.value) Text("Loading...", style = MaterialTheme.typography.bodyMedium)
        if (!loadingSent.value && sent.value.isEmpty()) Text("No sent requests", style = MaterialTheme.typography.bodyMedium)
        LazyColumn {
            items(sent.value) { req ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "To: ${req.receiverId}", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Message: ${req.message.takeIf { it.isNotBlank() } ?: "—"}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Status: ${req.status}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Connections
        Text("My Connections", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        if (loadingConnections.value) Text("Loading...", style = MaterialTheme.typography.bodyMedium)
        if (!loadingConnections.value && connections.value.isEmpty()) Text("No connections", style = MaterialTheme.typography.bodyMedium)
        LazyColumn {
            items(connections.value) { conn ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = conn.displayName.ifBlank { conn.id }, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Connections: ${conn.totalConnections}", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            // ask for confirmation before remove
                            targetConnectionId.value = conn.id
                            confirmAction.value = "remove"
                            showConfirm.value = true
                        }) { Text("Remove") }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirm.value) {
        val action = confirmAction.value
        val title = when (action) {
            "accept" -> "Accept request?"
            "reject" -> "Reject request?"
            "remove" -> "Remove connection?"
            else -> "Confirm"
        }
        val message = when (action) {
            "accept" -> "Are you sure you want to accept this mentorship request? This will create a mentorship connection."
            "reject" -> "Are you sure you want to reject this mentorship request?"
            "remove" -> "Are you sure you want to remove this connection?"
            else -> "Proceed?"
        }
        AlertDialog(
            onDismissRequest = { showConfirm.value = false; targetRequestId.value = null; targetConnectionId.value = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm.value = false
                    scope.launch {
                        when (action) {
                            "accept" -> {
                                targetRequestId.value?.let { id ->
                                    viewModel.acceptRequest(id).collect { r ->
                                        when (r) {
                                            is Resource.Success -> Toast.makeText(context, "Accepted", Toast.LENGTH_SHORT).show()
                                            is Resource.Error -> Toast.makeText(context, "Error: ${r.message}", Toast.LENGTH_SHORT).show()
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            "reject" -> {
                                targetRequestId.value?.let { id ->
                                    viewModel.rejectRequest(id).collect { r ->
                                        when (r) {
                                            is Resource.Success -> Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                                            is Resource.Error -> Toast.makeText(context, "Error: ${r.message}", Toast.LENGTH_SHORT).show()
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            "remove" -> {
                                targetConnectionId.value?.let { id ->
                                    viewModel.removeConnection(id).collect { r ->
                                        when (r) {
                                            is Resource.Success -> Toast.makeText(context, "Connection removed", Toast.LENGTH_SHORT).show()
                                            is Resource.Error -> Toast.makeText(context, "Error: ${r.message}", Toast.LENGTH_SHORT).show()
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm.value = false; targetRequestId.value = null; targetConnectionId.value = null }) { Text("No") }
            }
        )
    }
}

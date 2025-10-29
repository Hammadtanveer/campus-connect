package com.example.campusconnect.ui.theme

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.Resource
import com.example.campusconnect.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun MentorProfileScreen(mentorId: String?, viewModel: MainViewModel, navController: NavController) {
    val mentorState = remember { mutableStateOf<UserProfile?>(null) }
    val loading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val message = remember { mutableStateOf("") }
    val sending = remember { mutableStateOf(false) }
    val resultMessage = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // statuses
    val alreadyRequested = remember { mutableStateOf(false) }
    val connected = remember { mutableStateOf(false) }

    LaunchedEffect(mentorId) {
        if (mentorId == null) return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(mentorId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val u = doc.toObject(UserProfile::class.java)
                    mentorState.value = u?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                }
                loading.value = false
            }
            .addOnFailureListener { e ->
                error.value = e.message
                loading.value = false
            }
    }

    // collect sent requests and connections to compute status
    LaunchedEffect(mentorId) {
        if (mentorId == null) return@LaunchedEffect
        // check sent requests
        viewModel.getMyMentorshipRequests().collect { res ->
            when (res) {
                is Resource.Success -> {
                    alreadyRequested.value = res.data.any { it.receiverId == mentorId && it.status == "pending" }
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(mentorId) {
        if (mentorId == null) return@LaunchedEffect
        viewModel.getMyConnections().collect { res ->
            when (res) {
                is Resource.Success -> {
                    connected.value = res.data.any { it.id == mentorId }
                }
                else -> {}
            }
        }
    }

    val mentor = mentorState.value
    if (loading.value) {
        Text("Loading...", modifier = Modifier.padding(16.dp))
        return
    }
    if (mentor == null) {
        Text(text = "Mentor not found", modifier = Modifier.padding(16.dp))
        return
    }

    Card(modifier = Modifier.padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = mentor.displayName.ifBlank { "Unnamed" }, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mentor.mentorshipBio.takeIf { it.isNotBlank() } ?: mentor.bio.takeIf { it.isNotBlank() } ?: "No bio provided.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Expertise:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))

            // horizontal scrollable list of chips
            Row(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mentor.expertise.isEmpty()) {
                    Text(text = "Not specified", style = MaterialTheme.typography.bodySmall)
                } else {
                    mentor.expertise.forEach { ex ->
                        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Text(text = ex, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Status: ${mentor.mentorshipStatus}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = message.value, onValueChange = { message.value = it }, label = { Text("Message (optional)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val disableRequest = sending.value || alreadyRequested.value || connected.value || mentor.mentorshipStatus != "available"
                Button(onClick = {
                    if (disableRequest) return@Button
                    sending.value = true
                    resultMessage.value = null
                    scope.launch {
                        viewModel.sendMentorshipRequest(mentor.id, message.value).collect { res ->
                            when (res) {
                                is Resource.Loading -> sending.value = true
                                is Resource.Success -> {
                                    sending.value = false
                                    resultMessage.value = "Request sent"
                                    alreadyRequested.value = true
                                }
                                is Resource.Error -> {
                                    sending.value = false
                                    resultMessage.value = res.message ?: "Failed to send request"
                                }
                            }
                        }
                    }
                }, enabled = !disableRequest) {
                    Text(when {
                        connected.value -> "Connected"
                        alreadyRequested.value -> "Requested"
                        mentor.mentorshipStatus != "available" -> mentor.mentorshipStatus.capitalize()
                        else -> "Request Mentorship"
                    })
                }

                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }

            resultMessage.value?.let { rm ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = rm, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

package com.example.campusconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
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
import com.example.campusconnect.data.models.MentorshipRequest
import com.example.campusconnect.data.models.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RequestDetailScreen(requestId: String?, viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val reqState = remember { mutableStateOf<MentorshipRequest?>(null) }
    val loading = remember { mutableStateOf(true) }
    val actionLoading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // editable notes for mentors
    val notes = remember { mutableStateOf("") }

    LaunchedEffect(requestId) {
        if (requestId == null) return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()
        db.collection("mentorship_requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val r = doc.toObject(MentorshipRequest::class.java)
                    reqState.value = r?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                    notes.value = reqState.value?.connectionNotes ?: ""
                }
                loading.value = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load request: ${e.message}", Toast.LENGTH_SHORT).show()
                loading.value = false
            }
    }

    val req = reqState.value
    if (loading.value) {
        Text("Loading...", modifier = Modifier.padding(16.dp))
        return
    }
    if (req == null) {
        Text("Request not found", modifier = Modifier.padding(16.dp))
        return
    }

    // determine whether the current user is the receiver (mentor)
    val isReceiver = viewModel.userProfile?.id == req.receiverId

    Card(modifier = Modifier.padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "From: ${req.senderId}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Message:")
            Text(text = req.message.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Status: ${req.status}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))

            // If receiver and pending, allow adding connection notes before action
            if (req.status == "pending" && isReceiver) {
                Text(text = "Add connection notes (optional):")
                OutlinedTextField(value = notes.value, onValueChange = { notes.value = it }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))

                Column(horizontalAlignment = Alignment.Start) {
                    Button(onClick = {
                        actionLoading.value = true
                        scope.launch {
                            try {
                                // Update notes first (best-effort)
                                val db = FirebaseFirestore.getInstance()
                                val ref = db.collection("mentorship_requests").document(req.id)
                                ref.update("connectionNotes", notes.value).await()
                            } catch (e: Exception) {
                                // ignore note update failure; proceed to accept
                            }
                            // Collect accept flow in coroutine body (valid suspension)
                            viewModel.acceptRequest(req.id).collect { r ->
                                when (r) {
                                    is Resource.Success -> {
                                        actionLoading.value = false
                                        Toast.makeText(context, "Accepted", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                    is Resource.Error -> {
                                        actionLoading.value = false
                                        Toast.makeText(context, "Error: ${r.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }) { Text("Accept") }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        actionLoading.value = true
                        scope.launch {
                            viewModel.rejectRequest(req.id).collect { r ->
                                when (r) {
                                    is Resource.Success -> {
                                        actionLoading.value = false
                                        Toast.makeText(context, "Rejected", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                                    is Resource.Error -> {
                                        actionLoading.value = false
                                        Toast.makeText(context, "Error: ${r.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }) { Text("Reject") }
                }
            } else {
                // Non-receivers or non-pending: just show notes read-only
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Connection notes:")
                OutlinedTextField(value = req.connectionNotes, onValueChange = { /* read-only */ }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

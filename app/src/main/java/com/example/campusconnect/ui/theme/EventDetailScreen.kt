package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.OnlineEvent
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EventDetailScreen(eventId: String?, viewModel: MainViewModel, navController: NavController) {
    val eventState = remember { mutableStateOf<OnlineEvent?>(null) }
    val loading = remember { mutableStateOf(true) }

    LaunchedEffect(eventId) {
        if (eventId == null) return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val e = doc.toObject(OnlineEvent::class.java)
                    eventState.value = e
                }
                loading.value = false
            }
            .addOnFailureListener {
                loading.value = false
            }
    }

    val event = eventState.value
    if (loading.value) {
        Text("Loading...", modifier = Modifier.padding(16.dp))
        return
    }
    if (event == null) {
        Text("Event not found", modifier = Modifier.padding(16.dp))
        return
    }

    Card(modifier = Modifier.padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dt = event.dateTime?.toDate()?.let { sdf.format(it) } ?: "TBA"
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("When: $dt", style = MaterialTheme.typography.bodySmall)
                Text("Duration: ${event.durationMinutes} mins", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    viewModel.registerForEvent(event.id) { ok, err ->
                        if (ok) {
                            // Navigate back after successful registration
                            navController.popBackStack()
                        } else {
                            // TODO: show error snackbar
                        }
                    }
                }) {
                    Text("Register")
                }
            }
        }
    }
}

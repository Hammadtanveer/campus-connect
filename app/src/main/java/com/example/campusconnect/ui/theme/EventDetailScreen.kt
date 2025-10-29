package com.example.campusconnect.ui.theme

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.OnlineEvent
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.campusconnect.util.NetworkUtils
import androidx.core.net.toUri
import android.util.Log
import com.example.campusconnect.NotificationHelper

@Composable
fun EventDetailScreen(eventId: String?, viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val eventState = remember { mutableStateOf<OnlineEvent?>(null) }
    val loading = remember { mutableStateOf(true) }
    val participantCount = remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(eventId) {
        if (eventId == null) return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val e = doc.toObject(OnlineEvent::class.java)
                    eventState.value = e
                    // load participant count
                    db.collection("registrations").whereEqualTo("eventId", eventId).get()
                        .addOnSuccessListener { snap -> participantCount.value = snap.size() }
                        .addOnFailureListener { participantCount.value = null }
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
            Spacer(modifier = Modifier.height(8.dp))
            participantCount.value?.let { count ->
                Text("Participants: $count/${if (event.maxParticipants <= 0) "—" else event.maxParticipants}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons: Register / Join / Schedule / Share / Save Offline
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Register or Join depending on time
                Button(onClick = {
                    // If event has meet link and within join window, open meet; otherwise register
                    val now = System.currentTimeMillis()
                    val eventStart = event.dateTime?.toDate()?.time ?: 0L
                    val eventEnd = eventStart + (event.durationMinutes * 60_000L)
                    val canJoin = event.meetLink.isNotBlank() && now in (eventStart..eventEnd)
                    if (canJoin) {
                        // open meet link
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            // TODO: show snackbar; for now nothing
                            return@Button
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // fallback to browser (already attempted above) - log for debug
                            Log.w("EventDetail", "Failed to open meet link directly, attempting browser")
                            val browser = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                            browser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(browser)
                        }
                    } else {
                        // register for event
                        viewModel.registerForEvent(event.id) { ok, err ->
                            if (ok) {
                                // schedule reminder 30 minutes before
                                NotificationHelper.scheduleEventReminder(context, event)
                                navController.popBackStack()
                            } else {
                                // TODO: show error
                                Log.w("EventDetail", "registerForEvent failed: $err")
                            }
                        }
                    }
                }) {
                    val now = System.currentTimeMillis()
                    val eventStart = event.dateTime?.toDate()?.time ?: 0L
                    val eventEnd = eventStart + (event.durationMinutes * 60_000L)
                    Text(if (now in (eventStart..eventEnd)) "Join" else "Register")
                }

                Button(onClick = {
                    // Schedule reminder without registering
                    NotificationHelper.scheduleEventReminder(context, event)
                }) {
                    Text("Schedule Reminder")
                }

                Button(onClick = {
                    // Share event link and details
                    val shareText = "${event.title}\nWhen: $dt\nJoin: ${event.meetLink}"
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(shareIntent)
                }) {
                    Text("Share")
                }

                Button(onClick = {
                    // use existing addDownload API to add a lightweight entry
                    viewModel.addDownload("Event: ${event.title}", "--")
                }) {
                    Text("Save Offline")
                }
            }
        }
    }
}

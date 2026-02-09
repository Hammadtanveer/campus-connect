package com.example.campusconnect.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.example.campusconnect.data.models.OnlineEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.example.campusconnect.util.NetworkUtils
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.core.net.toUri
import com.example.campusconnect.NotificationHelper
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.ui.events.EventsViewModel
import com.example.campusconnect.data.models.Resource
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun EventDetailScreen(
    eventId: String?,
    navController: NavController,
    mainViewModel: MainViewModel,
    eventsViewModel: EventsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val eventState = remember { mutableStateOf<OnlineEvent?>(null) }
    val loading = remember { mutableStateOf(true) }
    val participantCount = remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(eventId) {
        if (eventId == null) return@LaunchedEffect

        loading.value = true
        val result = eventsViewModel.getEvent(eventId)
        if (result is Resource.Success) {
            eventState.value = result.data
            // load participant count
            val count = eventsViewModel.getParticipantCount(eventId)
            participantCount.value = count
        }
        loading.value = false
    }

    val event = eventState.value
    if (loading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (event == null) {
        Text("Event not found", modifier = Modifier.padding(16.dp))
        return
    }

    Card(modifier = Modifier.padding(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
            // ...existing code...
            // Action buttons: Register / Join / Schedule / Share / Save Offline
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Register or Join depending on time
                Button(onClick = {
                    val now = System.currentTimeMillis()
                    val eventStart = event.dateTime?.toDate()?.time ?: 0L
                    val eventEnd = eventStart + (event.durationMinutes * 60_000L)
                    val validDuration = event.durationMinutes > 0
                    val canJoin = validDuration && event.meetLink.isNotBlank() && now in (eventStart..eventEnd)

                    if (canJoin) {
                        // open meet link
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            Log.w("EventDetail", "Network unavailable, cannot open meet link")
                            return@Button
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Log.w("EventDetail", "Failed to open meet link directly, attempting browser")
                            try {
                                val browser = Intent(Intent.ACTION_VIEW, event.meetLink.toUri())
                                browser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(browser)
                            } catch (e: Exception) {
                                Log.e("EventDetail", "Failed to open meet link in browser", e)
                            }
                        }
                    } else {
                        // register for event
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            Log.w("EventDetail", "Network unavailable, cannot register for event")
                            return@Button
                        }
                        val effectiveEventId = event.id.ifBlank { eventId ?: "" }

                        eventsViewModel.registerForEvent(effectiveEventId) { ok, err ->
                            if (ok) {
                                // schedule reminder 30 minutes before
                                NotificationHelper.scheduleEventReminder(context, event)
                                navController.popBackStack()
                            } else {
                                Log.w("EventDetail", "registerForEvent failed: $err")
                            }
                        }
                    }
                }) {
                    val now = System.currentTimeMillis()
                    val eventStart = event.dateTime?.toDate()?.time ?: 0L
                    val eventEnd = eventStart + (event.durationMinutes * 60_000L)
                    val validDuration = event.durationMinutes > 0
                    Text(if (validDuration && now in (eventStart..eventEnd) && event.meetLink.isNotBlank()) "Join" else "Register")
                }

                // ...existing code...

                Button(onClick = {
                    // use existing addDownload API from MainViewModel
                    mainViewModel.addDownload("Event: ${event.title}", "--")
                }) {
                    Text("Save Offline")
                }
            }
        }
    }
}


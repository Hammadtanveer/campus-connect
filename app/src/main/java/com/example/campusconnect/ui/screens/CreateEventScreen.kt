package com.example.campusconnect.ui.screens

import android.widget.Toast
import com.example.campusconnect.data.models.EventType
import com.example.campusconnect.util.NetworkUtils
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.TimeoutCancellationException
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.ui.events.EventsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    viewModel: EventsViewModel = hiltViewModel()
) {
    if (!viewModel.canCreateEvent()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("You don't have permission to create events.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var meetLink by remember { mutableStateOf("") }
    var maxParticipants by remember { mutableStateOf("0") }
    var eventType by remember { mutableStateOf(EventType.ONLINE) }
    var venue by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Create Event", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // Event Type Selection
        Text("Event Type", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            RadioButton(
                selected = eventType == EventType.ONLINE,
                onClick = { eventType = EventType.ONLINE; venue = "" }
            )
            Text("Online", modifier = Modifier.clickable { eventType = EventType.ONLINE; venue = "" })
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = eventType == EventType.OFFLINE,
                onClick = { eventType = EventType.OFFLINE; meetLink = "" }
            )
            Text("Offline", modifier = Modifier.clickable { eventType = EventType.OFFLINE; meetLink = "" })
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = duration,
            onValueChange = { duration = it.filter { ch -> ch.isDigit() } },
            label = { Text("Duration (min)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = maxParticipants,
            onValueChange = { maxParticipants = it.filter { ch -> ch.isDigit() } },
            label = { Text("Max participants") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(8.dp))


        // Conditional fields
        if (eventType == EventType.ONLINE) {
            OutlinedTextField(
                value = meetLink,
                onValueChange = { meetLink = it },
                label = { Text("Meet link *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                isError = isSubmitting && meetLink.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (eventType == EventType.OFFLINE) {
            OutlinedTextField(
                value = venue,
                onValueChange = { venue = it },
                label = { Text("Venue / Address *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                isError = isSubmitting && venue.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                enabled = !isSubmitting,
                onClick = {
                    // Client-side validation
                    if (title.isBlank()) {
                        error = "Title is required"
                        return@Button
                    }
                    val dur = duration.toLongOrNull()
                    if (dur == null || dur <= 0L) {
                        error = "Duration must be a positive number"
                        return@Button
                    }
                    val maxP = maxParticipants.toIntOrNull() ?: 0

                    if (eventType == EventType.ONLINE && meetLink.isBlank()) {
                        error = "Meet link is required for ONLINE events"
                        return@Button
                    }
                    if (eventType == EventType.OFFLINE && venue.isBlank()) {
                        error = "Venue is required for OFFLINE events"
                        return@Button
                    }

                    // Quick validation - don't block on network check since Firestore handles offline gracefully
                    isSubmitting = true
                    error = null

                    scope.launch {
                        try {
                            if (!NetworkUtils.isNetworkAvailable(context)) {
                                isSubmitting = false
                                error = "No network connection. Please try again when online."
                                Toast.makeText(context, "No network connection", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val ts = Timestamp(Date())

                            val creationDeferred = async {
                                viewModel.createEventAwait(
                                    title = title,
                                    description = description,
                                    dateTime = ts,
                                    durationMinutes = dur,
                                    eventType = eventType,
                                    venue = venue,
                                    maxParticipants = maxP,
                                    meetLink = meetLink
                                )
                            }

                            creationDeferred.await()

                            Toast.makeText(context, "Event created successfully!", Toast.LENGTH_SHORT).show()

                            title = ""
                            description = ""
                            duration = "60"
                            maxParticipants = "0"
                            meetLink = ""
                            venue = ""
                            // Category reset removed
                            isSubmitting = false

                            navController.popBackStack()
                        } catch (_: TimeoutCancellationException) {
                            error = "Request timed out. Please try again if the event doesn't appear."
                            Toast.makeText(context, "Event creation timed out", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            val msg = e.message ?: "Failed to create event"
                            error = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            ) {
                if (isSubmitting) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                        Text("Creating event…")
                    }
                } else {
                    Text("Create")
                }
            }
        }
    }
}

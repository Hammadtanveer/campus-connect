package com.example.campusconnect.ui.screens

import android.widget.Toast
import com.example.campusconnect.data.models.EventCategory
import com.example.campusconnect.util.NetworkUtils
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.TimeoutCancellationException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var categoryExpanded by remember { mutableStateOf(false) }
    var categorySelected by remember { mutableStateOf(EventCategory.SOCIAL) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Create Event", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

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

        // Category dropdown using Box + DropdownMenu
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = categorySelected.name,
                onValueChange = {},
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSubmitting) { categoryExpanded = true },
                enabled = !isSubmitting
            )
            DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                EventCategory.entries.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.name) }, onClick = {
                        categorySelected = cat
                        categoryExpanded = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = meetLink,
            onValueChange = { meetLink = it },
            label = { Text("Meet link (optional)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                                    category = categorySelected,
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
                            categorySelected = EventCategory.SOCIAL
                            error = null

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

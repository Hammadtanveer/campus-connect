package com.hammadtanveer.campusconnect.ui.screens

import android.widget.Toast
import com.hammadtanveer.campusconnect.data.models.EventType
import com.hammadtanveer.campusconnect.util.NetworkUtils
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.ui.events.EventsViewModel
import com.hammadtanveer.campusconnect.ui.components.AppDatePickerDialog
import com.hammadtanveer.campusconnect.ui.components.AppTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    eventId: String? = null,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val isEditMode = !eventId.isNullOrBlank()
    val profile by viewModel.currentUserProfileFlow.collectAsStateWithLifecycle(null)

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
    var isLoadingEvent by remember { mutableStateOf(isEditMode) }
    var hasEditPermission by remember { mutableStateOf(!isEditMode) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance() }
    var selectedDateTime by remember { mutableStateOf<Date>(Date()) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    LaunchedEffect(eventId) {
        if (!isEditMode || eventId == null) return@LaunchedEffect
        isLoadingEvent = true
        when (val result = viewModel.getEvent(eventId)) {
            is Resource.Success -> {
                val event = result.data
                title = event.title
                description = event.description
                duration = event.duration.toString()
                maxParticipants = event.maxParticipants.toString()
                eventType = event.eventType
                meetLink = event.meetLink
                venue = event.venue
                selectedDateTime = event.dateTime?.toDate() ?: Date()
                hasEditPermission = viewModel.canEditEvent(event, profile)
                if (!hasEditPermission) {
                    error = "You don't have permission to edit this event."
                }
            }
            is Resource.Error -> {
                error = result.message ?: "Failed to load event"
                hasEditPermission = false
            }
            is Resource.Loading -> Unit
        }
        isLoadingEvent = false
    }

    if (!isEditMode && !viewModel.canCreateEvent(profile)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("You don't have permission to create events.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        }
        return
    }

    if (isEditMode && isLoadingEvent) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (isEditMode && !hasEditPermission) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                error ?: "You don't have permission to edit this event.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    Column(modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
        Text(text = if (isEditMode) "Edit Event" else "Create Event", style = MaterialTheme.typography.headlineMedium)
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

        // Date Picker
        OutlinedTextField(
            value = dateFormatter.format(selectedDateTime),
            onValueChange = {},
            label = { Text("Event Date") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            },
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Time Picker
        OutlinedTextField(
            value = timeFormatter.format(selectedDateTime),
            onValueChange = {},
            label = { Text("Event Time") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                }
            },
            enabled = !isSubmitting
        )

        if (showDatePicker) {
            AppDatePickerDialog(
                onDismiss = { showDatePicker = false },
                onDateSelected = { millis ->
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = millis
                    val newCal = Calendar.getInstance()
                    newCal.time = selectedDateTime
                    newCal.set(Calendar.YEAR, cal.get(Calendar.YEAR))
                    newCal.set(Calendar.MONTH, cal.get(Calendar.MONTH))
                    newCal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                    selectedDateTime = newCal.time
                    showDatePicker = false
                },
                initialDateMillis = selectedDateTime.time
            )
        }

        if (showTimePicker) {
            AppTimePickerDialog(
                onDismiss = { showTimePicker = false },
                onTimeSelected = { hour, minute ->
                    val cal = Calendar.getInstance()
                    cal.time = selectedDateTime
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    selectedDateTime = cal.time
                    showTimePicker = false
                },
                initialHour = calendar.apply { time = selectedDateTime }.get(Calendar.HOUR_OF_DAY),
                initialMinute = calendar.apply { time = selectedDateTime }.get(Calendar.MINUTE)
            )
        }

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

                            val durInt = dur.toInt()
                            val ts = Timestamp(selectedDateTime)

                            if (isEditMode) {
                                viewModel.updateEventAwait(
                                    eventId = eventId ?: return@launch,
                                    title = title,
                                    description = description,
                                    duration = durInt,
                                    eventType = eventType,
                                    venue = venue,
                                    maxParticipants = maxP,
                                    meetLink = meetLink
                                )
                                Toast.makeText(context, "Event updated successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.createEventAwait(
                                    title = title,
                                    description = description,
                                    dateTime = ts,
                                    duration = durInt,
                                    eventType = eventType,
                                    venue = venue,
                                    maxParticipants = maxP,
                                    meetLink = meetLink,
                                    profile = profile
                                )
                                Toast.makeText(context, "Event created successfully!", Toast.LENGTH_SHORT).show()
                            }

                            if (!isEditMode) {
                                title = ""
                                description = ""
                                duration = "60"
                                maxParticipants = "0"
                                meetLink = ""
                                venue = ""
                            }

                            navController.popBackStack()
                        } catch (_: TimeoutCancellationException) {
                            error = "Request timed out. Please try again if the event doesn't appear."
                            Toast.makeText(context, if (isEditMode) "Event update timed out" else "Event creation timed out", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            val msg = e.message ?: if (isEditMode) "Failed to update event" else "Failed to create event"
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
                        Text(if (isEditMode) "Saving changes..." else "Creating event...")
                    }
                } else {
                    Text(if (isEditMode) "Save" else "Create")
                }
            }
        }
    }
}

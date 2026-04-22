package com.hammadtanveer.campusconnect.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.ui.viewmodels.EventViewModel
import com.hammadtanveer.campusconnect.util.FileUtils
import com.hammadtanveer.campusconnect.ui.components.AppDatePickerDialog
import com.hammadtanveer.campusconnect.ui.components.AppTimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateSocietyEventScreen(
    societyId: String,
    societyName: String,
    eventId: String? = null,
    onEventSaved: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.currentUserProfileFlow.collectAsStateWithLifecycle(null)
    val isEditMode = !eventId.isNullOrBlank()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var coordinator by remember { mutableStateOf("") }
    var convener by remember { mutableStateOf("") }
    var register by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var posterPublicId by remember { mutableStateOf("") }
    var isPosterUploading by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateParser = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val timeParser = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val status = if (isEditMode) viewModel.updateEventStatus else viewModel.addEventStatus

    LaunchedEffect(eventId, societyId) {
        if (isEditMode) {
            viewModel.getSocietyEvent(societyId, eventId.orEmpty())
        } else {
            viewModel.clearSelectedEvent()
        }
    }

    LaunchedEffect(viewModel.selectedEvent?.id) {
        val selected = viewModel.selectedEvent
        if (isEditMode && selected != null && selected.id == eventId) {
            name = selected.name
            description = selected.description
            date = selected.date
            time = selected.time
            venue = selected.venue
            coordinator = selected.coordinator
            convener = selected.convener
            register = selected.register
            posterUrl = selected.posterUrl
            posterPublicId = selected.posterPublicId
        }
    }

    LaunchedEffect(status) {
        when (status) {
            is Resource.Success -> {
                Toast.makeText(context, if (isEditMode) "Event updated" else "Event created", Toast.LENGTH_SHORT).show()
                viewModel.resetStatus()
                onEventSaved()
            }
            is Resource.Error -> Toast.makeText(context, status.message ?: "Failed", Toast.LENGTH_SHORT).show()
            is Resource.Loading, null -> Unit
        }
    }

    val requiredAccess = if (isEditMode) {
        viewModel.canEditSocietyEvent(profile, societyId)
    } else {
        viewModel.canCreateSocietyEvent(profile, societyId)
    }
    if (!requiredAccess) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "You do not have permission to manage society events.",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    if (showDatePicker) {
        val initialMillis = try {
            dateParser.parse(date)?.time
        } catch (_: Exception) {
            null
        } ?: System.currentTimeMillis()

        AppDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { millis ->
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.timeInMillis = millis
                date = String.format("%02d-%02d-%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
                showDatePicker = false
            },
            initialDateMillis = initialMillis
        )
    }

    if (showTimePicker) {
        val initialTime = try {
            val d = timeParser.parse(time)
            val c = Calendar.getInstance()
            if (d != null) c.time = d
            c.get(Calendar.HOUR_OF_DAY) to c.get(Calendar.MINUTE)
        } catch (_: Exception) {
            12 to 0
        }

        AppTimePickerDialog(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                time = String.format("%02d:%02d", hour, minute)
                showTimePicker = false
            },
            initialHour = initialTime.first,
            initialMinute = initialTime.second
        )
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = FileUtils.getFileName(context, uri)
            val file = FileUtils.copyFileToCache(context, uri, fileName)
            if (file == null) {
                Toast.makeText(context, "Unable to process image", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            isPosterUploading = true
            viewModel.uploadPoster(societyId, file) { url, publicId, error ->
                isPosterUploading = false
                if (url != null) {
                    posterUrl = url
                    posterPublicId = publicId.orEmpty()
                    Toast.makeText(context, "Poster uploaded", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, error ?: "Poster upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val effectiveSocietyName = if (isEditMode) {
        viewModel.selectedEvent?.societyName?.ifBlank { societyName } ?: societyName
    } else {
        societyName
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isEditMode) "Edit $effectiveSocietyName Event" else "Create $effectiveSocietyName Event",
            style = MaterialTheme.typography.headlineMedium
        )

        if (posterUrl.isNotBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = "Event Poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
        }

        Button(
            onClick = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = !isPosterUploading && status !is Resource.Loading
        ) {
            if (isPosterUploading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(if (posterUrl.isBlank()) "Upload Event Poster" else "Change Event Poster")
            }
        }

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        PickerTextField(
            value = date,
            label = "Date of Event (dd-MM-yyyy)",
            onClick = { showDatePicker = true },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            }
        )
        PickerTextField(
            value = time,
            label = "Time of Event (HH:mm)",
            onClick = { showTimePicker = true },
            trailingIcon = {
                IconButton(onClick = { showTimePicker = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                }
            }
        )
        OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = coordinator, onValueChange = { coordinator = it }, label = { Text("Student Coordinator") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = convener, onValueChange = { convener = it }, label = { Text("Faculty Convener") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = register, onValueChange = { register = it }, label = { Text("Registration Link (Google Form URL)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (
                    name.isBlank() || description.isBlank() || date.isBlank() || time.isBlank() || venue.isBlank() ||
                    coordinator.isBlank() || convener.isBlank() || register.isBlank() || posterUrl.isBlank()
                ) {
                    Toast.makeText(context, "Please fill all fields and upload poster", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (isEditMode) {
                    val existing = viewModel.selectedEvent
                    if (existing == null) {
                        Toast.makeText(context, "Event data unavailable", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.updateEvent(
                        societyId = societyId,
                        eventId = eventId.orEmpty(),
                        profile = profile,
                        updated = existing.copy(
                            societyId = societyId,
                            societyName = effectiveSocietyName,
                            name = name,
                            description = description,
                            date = date,
                            time = time,
                            venue = venue,
                            coordinator = coordinator,
                            convener = convener,
                            register = register,
                            posterUrl = posterUrl,
                            posterPublicId = posterPublicId
                        )
                    )
                } else {
                    viewModel.createEvent(
                        societyId = societyId,
                        societyName = effectiveSocietyName,
                        name = name,
                        description = description,
                        date = date,
                        time = time,
                        venue = venue,
                        coordinator = coordinator,
                        convener = convener,
                        register = register,
                        posterUrl = posterUrl,
                        posterPublicId = posterPublicId,
                        profile = profile
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isPosterUploading && status !is Resource.Loading
        ) {
            if (status is Resource.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditMode) "Save Changes" else "Post Event")
            }
        }
    }
}

@Composable
private fun PickerTextField(
    value: String,
    label: String,
    onClick: () -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = trailingIcon
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

package com.example.campusconnect.ui.theme

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.EventCategory
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.util.NetworkUtils
import com.google.firebase.Timestamp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val title = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val duration = remember { mutableStateOf("60") }
    val meetLink = remember { mutableStateOf("") }
    val maxParticipants = remember { mutableStateOf("0") }
    val categoryExpanded = remember { mutableStateOf(false) }
    val categorySelected = remember { mutableStateOf(EventCategory.SOCIAL) }
    val error = remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Create Event", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = title.value,
            onValueChange = { title.value = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description.value,
            onValueChange = { description.value = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stack duration and max participants vertically to avoid modifier.weight
        OutlinedTextField(
            value = duration.value,
            onValueChange = { duration.value = it.filter { ch -> ch.isDigit() } },
            label = { Text("Duration (min)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = maxParticipants.value,
            onValueChange = { maxParticipants.value = it.filter { ch -> ch.isDigit() } },
            label = { Text("Max participants") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category dropdown using Box + DropdownMenu
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = categorySelected.value.name,
                onValueChange = {},
                label = { Text("Category") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categoryExpanded.value = true }
            )
            DropdownMenu(expanded = categoryExpanded.value, onDismissRequest = { categoryExpanded.value = false }) {
                EventCategory.entries.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.name) }, onClick = {
                        categorySelected.value = cat
                        categoryExpanded.value = false
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = meetLink.value,
            onValueChange = { meetLink.value = it },
            label = { Text("Meet link (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        error.value?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                // Basic validation
                if (title.value.isBlank()) {
                    error.value = "Title is required"
                    return@Button
                }

                if (!NetworkUtils.isNetworkAvailable(context)) {
                    error.value = "No network connection. Please try again when online."
                    return@Button
                }

                val dur = duration.value.toLongOrNull() ?: 60L
                val maxP = maxParticipants.value.toIntOrNull() ?: 0
                val ts = Timestamp(Date())
                viewModel.createEvent(
                    title = title.value,
                    description = description.value,
                    dateTime = ts,
                    durationMinutes = dur,
                    category = categorySelected.value,
                    maxParticipants = maxP,
                    meetLink = meetLink.value
                ) { ok, errMsg ->
                    if (ok) navController.popBackStack()
                    else error.value = errMsg
                }
            }) {
                Text("Create")
            }
        }
    }
}

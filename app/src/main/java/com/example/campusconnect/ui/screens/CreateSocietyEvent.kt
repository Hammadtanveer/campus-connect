package com.example.campusconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.ui.viewmodels.EventViewModel

@Composable
fun CreateEventScreen(
    onEventCreated: () -> Unit,
    viewModel: EventViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var coordinator by remember { mutableStateOf("") }
    var convener by remember { mutableStateOf("") }
    var register by remember { mutableStateOf("") }


    val context = LocalContext.current
    val status = viewModel.addEventStatus

    LaunchedEffect(status) {
        if (status is Resource.Success) {
            val newId = status.data // The ID returned from Firestore
//            navController.navigate("society_event_detail/$newId")
            Toast.makeText(context, "Event Created!", Toast.LENGTH_SHORT).show()
            onEventCreated()
            viewModel.resetStatus()
        } else if (status is Resource.Error) {
            Toast.makeText(context, "Error: ${status.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create New Event", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Event Nme") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date of Event") },
            modifier = Modifier.fillMaxWidth(),
//            minLines = 3
        )

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time of Event") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = venue,
            onValueChange = { venue = it },
            label = { Text("Venue") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = coordinator,
            onValueChange = { coordinator = it },
            label = { Text("Student Coordinator") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = convener,
            onValueChange = { convener = it },
            label = { Text("Faculty Convener") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = register,
            onValueChange = { register = it },
            label = { Text("Registration Link") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isBlank() || date.isBlank() || time.isBlank() || venue.isBlank() || coordinator.isBlank() || convener.isBlank() || register.isBlank()) {
                    Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.createEvent(name, date, time, venue, coordinator, convener, register)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = status !is Resource.Loading
        ) {
            if (status is Resource.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Post Event")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CreateEventScreenPreview() {
    CreateEventScreen(onEventCreated = {})
}
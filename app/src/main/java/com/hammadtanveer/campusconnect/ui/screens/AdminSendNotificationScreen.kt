
package com.hammadtanveer.campusconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.ui.viewmodels.AdminSendNotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSendNotificationScreen(
    navController: NavController,
    viewModel: AdminSendNotificationViewModel = hiltViewModel()
) {
    val sendState by viewModel.sendState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val topicOptions = viewModel.topicOptions
    var selectedTopic by remember { mutableStateOf(topicOptions.first()) }
    var topicExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val isSending = sendState is Resource.Loading

    LaunchedEffect(sendState) {
        when (val state = sendState) {
            is Resource.Success -> {
                Toast.makeText(context, "Notification queued successfully", Toast.LENGTH_SHORT).show()
                title = ""
                body = ""
                viewModel.resetSendState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(state.message ?: "Failed to queue notification")
                viewModel.resetSendState()
            }
            is Resource.Loading, null -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Send Notification") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = topicExpanded,
                onExpandedChange = { topicExpanded = !topicExpanded }
            ) {
                OutlinedTextField(
                    value = selectedTopic.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Topic") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = topicExpanded,
                    onDismissRequest = { topicExpanded = false }
                ) {
                    topicOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                selectedTopic = option
                                topicExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSending
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                enabled = !isSending
            )

            Button(
                onClick = { viewModel.sendNotification(selectedTopic, title, body) },
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text("Queueing...")
                } else {
                    Text("Queue Notification")
                }
            }
        }
    }
}


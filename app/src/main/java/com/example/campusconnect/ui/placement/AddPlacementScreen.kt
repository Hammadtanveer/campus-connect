package com.example.campusconnect.ui.placement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.data.models.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlacementScreen(
    navController: NavController,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    var companyName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eligibility by remember { mutableStateOf("") }
    var applyLink by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post New Job") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Job Role / Profile") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = salary,
                onValueChange = { salary = it },
                label = { Text("Salary / Package (e.g. 12 LPA)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = eligibility,
                onValueChange = { eligibility = it },
                label = { Text("Eligibility Criteria") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 10
            )

            OutlinedTextField(
                value = applyLink,
                onValueChange = { applyLink = it },
                label = { Text("Application Link (URL)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                singleLine = true
            )

            Button(
                onClick = {
                    if (companyName.isBlank() || role.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Company and Role are required") }
                        return@Button
                    }

                    isSubmitting = true
                    val placement = Placement(
                        companyName = companyName,
                        role = role,
                        salary = salary,
                        description = description,
                        eligibilityCriteria = eligibility,
                        applyLink = applyLink,
                        location = location
                        // postedDate is auto-set in model
                    )

                    viewModel.addPlacement(placement)

                    // Optimistic navigation or listen to side effects
                    // Ideally we'd wait for success, but for MVP popping back is okay
                    // A better way is to pass a callback to VM or observe state
                    // We'll just assume success for now or close closely
                    scope.launch {
                        snackbarHostState.showSnackbar("Posting Job...")
                        // Simulate delay or wait for VM (requires exposing a status flow)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Post Placement Notice")
                }
            }
        }
    }
}

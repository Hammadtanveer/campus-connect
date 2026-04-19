package com.hammadtanveer.campusconnect.ui.placement

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
import com.hammadtanveer.campusconnect.data.models.Placement
import com.hammadtanveer.campusconnect.data.models.Resource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlacementScreen(
    navController: NavController,
    placementId: String? = null,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    val isEditMode = !placementId.isNullOrBlank()
    var companyName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eligibility by remember { mutableStateOf("") }
    var applyLink by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var postedDate by remember { mutableStateOf(java.util.Date()) }

    var isLoadingPlacement by remember { mutableStateOf(isEditMode) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveStatus by viewModel.savePlacementStatus.collectAsState()

    LaunchedEffect(placementId) {
        if (!isEditMode || placementId == null) return@LaunchedEffect
        isLoadingPlacement = true
        when (val result = viewModel.getPlacement(placementId)) {
            is Resource.Success -> {
                val placement = result.data
                companyName = placement.companyName
                role = placement.role
                salary = placement.salary
                description = placement.description
                eligibility = placement.eligibilityCriteria
                applyLink = placement.applyLink
                location = placement.location
                postedDate = placement.postedDate
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(result.message ?: "Failed to load placement")
            }
            is Resource.Loading -> Unit
        }
        isLoadingPlacement = false
    }

    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(if (isEditMode) "Placement updated" else "Placement posted")
                viewModel.resetSavePlacementStatus()
                navController.popBackStack()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(status.message ?: "Save failed")
                viewModel.resetSavePlacementStatus()
            }
            is Resource.Loading, null -> Unit
        }
    }

    val isSubmitting = saveStatus is Resource.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Job" else "Post New Job") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoadingPlacement) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

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

                    val placement = Placement(
                        id = placementId.orEmpty(),
                        companyName = companyName,
                        role = role,
                        salary = salary,
                        description = description,
                        eligibilityCriteria = eligibility,
                        applyLink = applyLink,
                        location = location,
                        postedDate = postedDate
                    )

                    if (isEditMode) {
                        viewModel.updatePlacement(placementId = placementId.orEmpty(), placement = placement)
                    } else {
                        viewModel.addPlacement(placement)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEditMode) "Save Changes" else "Post Placement Notice")
                }
            }
        }
    }
}

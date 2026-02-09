package com.example.campusconnect.ui.placement

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.data.models.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementDetailScreen(
    placementId: String,
    navController: NavController,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var placementState by remember { mutableStateOf<Resource<Placement>>(Resource.Loading) }

    LaunchedEffect(placementId) {
        placementState = viewModel.getPlacement(placementId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = placementState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is Resource.Success -> {
                    val placement = state.data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = placement.role,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = placement.companyName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider()

                        DetailRow("Location", placement.location.ifBlank { "Not specified" })
                        DetailRow("Salary / Package", placement.salary.ifBlank { "Not specified" })

                        if (placement.eligibilityCriteria.isNotBlank()) {
                            Text("Eligibility Criteria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(placement.eligibilityCriteria, style = MaterialTheme.typography.bodyMedium)
                        }

                        Text("Job Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(placement.description, style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (placement.applyLink.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(placement.applyLink))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = placement.applyLink.isNotBlank()
                        ) {
                            Text("Apply Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

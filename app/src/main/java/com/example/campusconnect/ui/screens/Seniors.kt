package com.example.campusconnect.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.Senior
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.security.canAddSenior
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun Seniors(viewModel: MainViewModel, navController: NavController) {
    val seniors = viewModel.seniorsList
    val deleteStatus = viewModel.deleteSeniorStatus
    val canAddSenior = viewModel.userProfile?.canAddSenior() == true
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deleteStatus) {
        when (deleteStatus) {
            is Resource.Success<*> -> {
                snackbarHostState.showSnackbar("Senior deleted")
                viewModel.resetDeleteSeniorStatus()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(deleteStatus.message ?: "Failed to delete senior")
                viewModel.resetDeleteSeniorStatus()
            }
            is Resource.Loading, null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (canAddSenior) {
                FloatingActionButton(onClick = { navController.navigate("senior_add") }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Senior")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = seniors,
                key = { it.id }
            ) { senior ->
                SeniorItem(senior = senior, onClick = {
                    navController.navigate("senior_detail/${senior.id}")
                })
            }
            if (seniors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyMessage = if (canAddSenior) {
                            "No seniors found. Add one!"
                        } else {
                            "No seniors found yet."
                        }
                        Text(emptyMessage)
                    }
                }
            }
        }
    }
}

@Composable
fun SeniorItem(senior: Senior, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = senior.name,
                    modifier = Modifier.padding(end = 12.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = senior.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${senior.branch} - ${senior.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Arrow Right",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

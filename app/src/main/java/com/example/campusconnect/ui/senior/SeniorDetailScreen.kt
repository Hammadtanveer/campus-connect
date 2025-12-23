package com.example.campusconnect.ui.senior

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusconnect.data.Senior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorDetailScreen(
    senior: Senior,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(senior.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Senior")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Image
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                if (senior.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = senior.photoUrl,
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No Photo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Name: ${senior.name}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Branch: ${senior.branch}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Year: ${senior.year}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Mobile: ${senior.mobileNumber}", style = MaterialTheme.typography.bodyLarge)

            if (senior.linkedinUrl.isNotEmpty()) {
                Text(text = "LinkedIn: ${senior.linkedinUrl}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Bio:", style = MaterialTheme.typography.titleMedium)
            Text(text = senior.bio, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


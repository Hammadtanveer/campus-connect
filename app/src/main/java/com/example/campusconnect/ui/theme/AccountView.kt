package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel

@Composable
fun AccountView(viewModel: MainViewModel) {
    val userProfile = viewModel.userProfile.value
    if (userProfile == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Basic User Info (Name, Email)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile Icon",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = userProfile.displayName.ifBlank { "Student Name" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = userProfile.email,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            IconButton(onClick = { /* TODO: Navigate to an EditProfileScreen */ }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Profile"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // Academic Details Section
        if (userProfile.course.isNotBlank() || userProfile.branch.isNotBlank() || userProfile.year.isNotBlank()) {
            Text("Academic Information", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Course Info
            if (userProfile.course.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info, // <<< CHANGED ICON
                        contentDescription = "Course Information", // Updated content description
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp)
                    )
                    Column {
                        Text("Course", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = userProfile.course,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Branch Info
            if (userProfile.branch.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info, // <<< CHANGED ICON
                        contentDescription = "Branch Information", // Updated content description
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp)
                    )
                    Column {
                        Text("Branch", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = userProfile.branch,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Year Info
            if (userProfile.year.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info, // <<< CHANGED ICON
                        contentDescription = "Year Information", // Updated content description
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 12.dp)
                    )
                    Column {
                        Text("Year", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = userProfile.year,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.signOut() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text("Sign Out")
        }
    }
}

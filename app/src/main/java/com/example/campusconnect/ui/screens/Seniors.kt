package com.example.campusconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.security.canUpdateSenior

@Composable
fun Seniors(viewModel: MainViewModel? = null) {
    val names = listOf(
        "Hammad Tanveer",
        "Mohd Faisal",
        "Mohammad Adnan",
        "Mohd Arham",
        "Harsh Kumar"
    )

    LazyColumn {
        items(names.size) { index ->
            LibItem(name = names[index])
        }
    }

    if (viewModel?.userProfile?.canUpdateSenior() == true) {
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.updateSeniorProfile(seniorId = 1, field = "bio", newValue = "Updated") { _, _ -> } }) {
            Text("Quick Senior Update (Demo)")
        }
    }
}

@Composable
fun LibItem(name: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = name,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Text(text = name, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Arrow Right",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

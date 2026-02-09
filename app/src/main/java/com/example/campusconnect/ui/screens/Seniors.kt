package com.example.campusconnect.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.Senior

@Composable
fun Seniors(viewModel: MainViewModel, navController: NavController) {
    val seniors = viewModel.seniorsList

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("senior_add") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Senior")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(seniors.size) { index ->
                val senior = seniors[index]
                SeniorItem(senior = senior, onClick = {
                    navController.navigate("senior_detail/${senior.id}")
                })
            }
            if (seniors.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No seniors found. Add one!")
                    }
                }
            }
        }
    }
}

@Composable
fun SeniorItem(senior: Senior, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = senior.name,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(text = senior.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${senior.branch} - ${senior.year}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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

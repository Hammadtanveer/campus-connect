package com.example.campusconnect.ui.screens
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campusconnect.data.models.Placement
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.ui.placement.PlacementViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.campusconnect.MainViewModel

@Composable
fun PlacementCareerScreen(
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: PlacementViewModel = hiltViewModel()
) {
    val placementsState by viewModel.placements.collectAsState()
    val userProfile = mainViewModel.userProfile

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Placements", style = MaterialTheme.typography.headlineMedium)

            if (userProfile?.isAdmin == true) {
                Button(onClick = { navController.navigate("placement/add") }) {
                    Text("Post Job")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (val state = placementsState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Text(text = "Error: ", color = MaterialTheme.colorScheme.error)
            }
            is Resource.Success -> {
                 if (state.data.isEmpty()) {
                    Text("No job postings available.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.data) { placement ->
                            PlacementListItem(placement, onClick = { navController.navigate("placement/${placement.id}") })
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PlacementListItem(placement: Placement, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text(
                     text = placement.role.ifBlank { "Role TBA" }, 
                     style = MaterialTheme.typography.titleMedium, 
                     fontWeight = FontWeight.Bold,
                     color = MaterialTheme.colorScheme.onSurface
                 )
                 Text(
                     text = placement.salary.ifBlank { "Salary N/A" }, 
                     style = MaterialTheme.typography.labelMedium, 
                     color = MaterialTheme.colorScheme.primary
                 )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = placement.companyName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = placement.description, 
                maxLines = 3, 
                overflow = TextOverflow.Ellipsis, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
             Spacer(modifier = Modifier.height(12.dp))
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
                 modifier = Modifier.align(Alignment.End),
                 enabled = placement.applyLink.isNotBlank()
             ) {
                 Text("Apply Now")
             }
        }
    }
}

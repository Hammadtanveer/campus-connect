package com.hammadtanveer.campusconnect.ui.screens

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hammadtanveer.campusconnect.MainViewModel
import com.hammadtanveer.campusconnect.data.models.SeniorProfile
import com.hammadtanveer.campusconnect.data.models.Resource
import com.hammadtanveer.campusconnect.security.canAddSenior
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hammadtanveer.campusconnect.util.DbgLog
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.CachePolicy
import com.hammadtanveer.campusconnect.R
import com.hammadtanveer.campusconnect.util.formatTimestamp
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

@Composable
fun Seniors(viewModel: MainViewModel, navController: NavController) {
    val seniors by viewModel.seniorsList.collectAsStateWithLifecycle(emptyList())
    val profile by viewModel.sessionProfileFlow.collectAsStateWithLifecycle(null)
    val deleteStatus = viewModel.deleteSeniorStatus
    val canAddSenior = profile?.canAddSenior() == true
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        DbgLog.d("UI", "Seniors screen enter")
        onDispose { DbgLog.d("UI", "Seniors screen exit") }
    }

    LaunchedEffect(seniors.size, canAddSenior) {
        DbgLog.d("UI", "Seniors render state count=${seniors.size} canAddSenior=$canAddSenior")
    }

    LaunchedEffect(seniors.size) {
        DbgLog.d("UI", "Seniors collector emission count=${seniors.size}")
    }

    LaunchedEffect(deleteStatus) {
        DbgLog.d("UI", "deleteStatus changed=$deleteStatus")
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
                DbgLog.d("UI", "Seniors empty state shown canAddSenior=$canAddSenior")
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emptyMessage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeniorItem(
    senior: SeniorProfile,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (senior.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(senior.profileImageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = { /* log error */ }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Details column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = senior.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(2.dp))

                // Branch + Batch
                Text(
                    text = if (senior.batch.isNotBlank()) "${senior.branch} • ${senior.batch}" else senior.branch,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Company placed (if available)
                if (senior.companyPlaced.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = senior.companyPlaced,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Bio preview (if available)
                if (senior.bio.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = senior.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.example.campusconnect.ui.senior

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.Senior
import com.example.campusconnect.util.FileUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorAddScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onAddClick: (Senior) -> Unit
) {
    // 1. State variables start empty for a new entry
    var name by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            val fileName = FileUtils.getFileName(context, uri)
            val file = FileUtils.copyFileToCache(context, uri, fileName)
            if (file != null) {
                viewModel.uploadSeniorImage(file) { url ->
                    isUploading = false
                    if (url != null) {
                        photoUrl = url
                    }
                }
            } else {
                isUploading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Senior") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image Upload
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Senior Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = 40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Upload Photo",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                if (isUploading) {
                    CircularProgressIndicator()
                }
            }

            // Input Fields
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = linkedin, onValueChange = { linkedin = it }, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. The Add Button
            Button(
                onClick = {
                    // Create a NEW Senior object
                    // We generate a random ID here so it can be identified in the list
                    val newSenior = Senior(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        branch = branch,
                        year = year,
                        mobileNumber = mobile,
                        bio = bio,
                        linkedinUrl = linkedin,
                        photoUrl = photoUrl // Use uploaded URL
                    )
                    onAddClick(newSenior)
                },
                modifier = Modifier.fillMaxWidth(),
                // Simple validation: Disable button if name is empty
                enabled = name.isNotBlank()
            ) {
                Text("Add Senior")
            }
        }
    }
}

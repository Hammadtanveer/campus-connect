package com.example.campusconnect.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.data.models.UploadProgress
import com.example.campusconnect.security.canUploadNotes
import com.example.campusconnect.ui.viewmodels.UploadNoteViewModel
import com.example.campusconnect.util.Constants
import com.example.campusconnect.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadNoteScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    val viewModel: UploadNoteViewModel = hiltViewModel()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val fileSize by viewModel.fileSize.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val canUpload = mainViewModel.userProfile?.canUploadNotes() == true

    var title by rememberSaveable { mutableStateOf("") }
    var subjectCode by rememberSaveable { mutableStateOf("") }
    var subjectName by rememberSaveable { mutableStateOf("") }
    var branch by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var semesterExpanded by remember { mutableStateOf(false) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    val isUploading = uploadProgress is UploadProgress.Uploading

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectFile(context, it) }
    }

    LaunchedEffect(uploadProgress) {
        when (val state = uploadProgress) {
            is UploadProgress.Success -> {
                snackbarHostState.showSnackbar("Note uploaded successfully")
                viewModel.resetUpload()
                onUploadSuccess()
            }
            is UploadProgress.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (!canUpload) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Access denied",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Only admin and super admin can upload notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateBack) { Text("Go back") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subject Name first (title removed from UI)
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject Name *") },
                placeholder = { Text("DevOps and Automation") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = showValidationErrors && subjectName.isBlank(),
                enabled = !isUploading
            )

            OutlinedTextField(
                value = subjectCode,
                onValueChange = { subjectCode = it.uppercase() },
                label = { Text("Subject Code *") },
                placeholder = { Text("CSE702") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = showValidationErrors && subjectCode.isBlank(),
                enabled = !isUploading
            )

            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it.uppercase() },
                label = { Text("Branch / Department *") },
                placeholder = { Text("CSE") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = showValidationErrors && branch.isBlank(),
                enabled = !isUploading
            )

            ExposedDropdownMenuBox(
                expanded = semesterExpanded,
                onExpandedChange = { if (!isUploading) semesterExpanded = !semesterExpanded }
            ) {
                OutlinedTextField(
                    value = semester,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Semester *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = semesterExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    isError = showValidationErrors && semester.isBlank(),
                    enabled = !isUploading
                )
                ExposedDropdownMenu(
                    expanded = semesterExpanded,
                    onDismissRequest = { semesterExpanded = false }
                ) {
                    Constants.SEMESTERS.forEach { sem ->
                        DropdownMenuItem(
                            text = { Text(sem) },
                            onClick = {
                                semester = sem
                                semesterExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Short note about contents...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isUploading
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Done, contentDescription = null)
                        Text("Upload PDF (Max 20MB)", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { filePicker.launch("application/pdf") },
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileUri == null) "Choose PDF" else "Change PDF")
                    }

                    if (selectedFileUri != null) {
                        Text("File: $fileName", style = MaterialTheme.typography.bodyMedium)
                        Text("Size: ${FileUtils.formatFileSize(fileSize)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (isUploading) {
                val progress = (uploadProgress as UploadProgress.Uploading).progress
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Text("Uploading... $progress%")
                }
            }

            Button(
                onClick = {
                    showValidationErrors = true
                    val hasRequired =
                        subjectCode.isNotBlank() &&
                        subjectName.isNotBlank() &&
                        branch.isNotBlank() &&
                        semester.isNotBlank() &&
                        selectedFileUri != null

                    if (hasRequired) {
                        title = "${subjectCode.trim()} - ${subjectName.trim()}"
                        viewModel.uploadNote(
                            context = context,
                            title = title,
                            description = description,
                            subjectCode = subjectCode,
                            subjectName = subjectName,
                            branch = branch,
                            semester = semester
                        )
                    }
                },
                enabled = !isUploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit")
            }
        }
    }
}

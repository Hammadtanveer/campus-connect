package com.example.campusconnect.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.campusconnect.data.models.Note
import com.example.campusconnect.data.models.UploadProgress
import com.example.campusconnect.ui.viewmodels.NotesViewModel
import com.example.campusconnect.ui.viewmodels.UploadNoteViewModel
import com.example.campusconnect.util.Constants
import com.example.campusconnect.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController? = null) {
    val notesViewModel: NotesViewModel = hiltViewModel()
    val uploadViewModel: UploadNoteViewModel = hiltViewModel()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All Notes", "My Uploads", "Upload")

    // Navigate to upload screen when Upload tab is selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 && navController != null) {
            navController.navigate("upload_note")
            selectedTab = 0 // Reset to All Notes tab
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> AllNotesTab(notesViewModel)
                1 -> MyNotesTab(notesViewModel)
                2 -> {
                    // Show placeholder if navigation didn't trigger
                    if (navController == null) {
                        UploadTab(uploadViewModel) {
                            selectedTab = 1
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllNotesTab(viewModel: NotesViewModel) {
    val state by viewModel.allNotesState.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Filters
        FilterSection(
            selectedSubject = selectedSubject,
            selectedSemester = selectedSemester,
            searchQuery = searchQuery,
            onSubjectChange = { viewModel.setSubjectFilter(it) },
            onSemesterChange = { viewModel.setSemesterFilter(it) },
            onSearchChange = { viewModel.setSearchQuery(it) },
            onClearFilters = { viewModel.clearFilters() }
        )

        HorizontalDivider()

        // Notes List
        NotesListContent(
            state = state,
            onDownload = { note ->
                viewModel.recordDownload(note.id)
            },
            onDelete = null // Can't delete others' notes
        )
    }
}

@Composable
fun MyNotesTab(viewModel: NotesViewModel) {
    val state by viewModel.myNotesState.collectAsState()
    val deleteInProgress by viewModel.deleteInProgress.collectAsState()

    NotesListContent(
        state = state,
        onDownload = { note ->
            viewModel.recordDownload(note.id)
        },
        onDelete = { note ->
            viewModel.deleteNote(note)
        },
        deleteInProgress = deleteInProgress
    )
}

@Composable
fun NotesListContent(
    state: com.example.campusconnect.ui.state.UiState<List<Note>>,
    onDownload: (Note) -> Unit,
    onDelete: ((Note) -> Unit)? = null,
    deleteInProgress: String? = null
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            is com.example.campusconnect.ui.state.UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is com.example.campusconnect.ui.state.UiState.Error -> {
                ErrorMessage(
                    message = state.message,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is com.example.campusconnect.ui.state.UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        message = "No notes available",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.data) { note ->
                            NoteCard(
                                note = note,
                                onDownload = {
                                    onDownload(note)
                                    // Open file in browser/viewer
                                    val intent = Intent(Intent.ACTION_VIEW, note.fileUrl.toUri())
                                    context.startActivity(intent)
                                },
                                onDelete = onDelete?.let { { onDelete(note) } },
                                isDeleting = deleteInProgress == note.id
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onDownload: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isDeleting: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDownload() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (note.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // File type icon
                FileTypeIcon(note.fileType)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip(text = note.subject, icon = Icons.Default.Star)
                InfoChip(text = note.semester, icon = Icons.Default.DateRange)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FileUtils.formatFileSize(note.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${note.downloads} downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Uploader info
            Text(
                text = "Uploaded by ${note.uploaderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDownload) {
                    Icon(Icons.Default.Share, contentDescription = "Download")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open")
                }

                onDelete?.let {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(start = 8.dp)
                        )
                    } else {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete '${note.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileTypeIcon(fileType: String) {
    val icon = when (fileType) {
        "pdf" -> Icons.Default.Email
        "image" -> Icons.Default.AccountCircle
        "document" -> Icons.Default.Email
        "presentation" -> Icons.Default.Info
        "spreadsheet" -> Icons.Default.Menu
        else -> Icons.Default.Info
    }

    Icon(
        imageVector = icon,
        contentDescription = fileType,
        modifier = Modifier.size(32.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun InfoChip(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    selectedSubject: String?,
    selectedSemester: String?,
    searchQuery: String?,
    onSubjectChange: (String?) -> Unit,
    onSemesterChange: (String?) -> Unit,
    onSearchChange: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    var searchText by remember { mutableStateOf(searchQuery ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onSearchChange(if (it.isBlank()) null else it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        searchText = ""
                        onSearchChange(null)
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filters:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedSubject != null || selectedSemester != null) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }
        }

        // Subject filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(Constants.SUBJECTS) { subject ->
                FilterChip(
                    selected = selectedSubject == subject,
                    onClick = {
                        onSubjectChange(if (selectedSubject == subject) null else subject)
                    },
                    label = { Text(subject) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Semester filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(Constants.SEMESTERS) { semester ->
                FilterChip(
                    selected = selectedSemester == semester,
                    onClick = {
                        onSemesterChange(if (selectedSemester == semester) null else semester)
                    },
                    label = { Text(semester) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadTab(
    viewModel: UploadNoteViewModel,
    onUploadSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val fileSize by viewModel.fileSize.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var showSubjectMenu by remember { mutableStateOf(false) }
    var showSemesterMenu by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.selectFile(context, it)
        }
    }

    // Handle upload success
    LaunchedEffect(uploadProgress) {
        if (uploadProgress is UploadProgress.Success) {
            onUploadSuccess()
            // Reset form
            title = ""
            description = ""
            selectedSubject = ""
            selectedSemester = ""
            viewModel.resetUpload()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = uploadProgress !is UploadProgress.Uploading
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            enabled = uploadProgress !is UploadProgress.Uploading
        )

        // Subject dropdown
        ExposedDropdownMenuBox(
            expanded = showSubjectMenu,
            onExpandedChange = {
                if (uploadProgress !is UploadProgress.Uploading) {
                    showSubjectMenu = !showSubjectMenu
                }
            }
        ) {
            OutlinedTextField(
                value = selectedSubject,
                onValueChange = {},
                readOnly = true,
                label = { Text("Subject *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                enabled = uploadProgress !is UploadProgress.Uploading
            )

            ExposedDropdownMenu(
                expanded = showSubjectMenu,
                onDismissRequest = { showSubjectMenu = false }
            ) {
                Constants.SUBJECTS.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject) },
                        onClick = {
                            selectedSubject = subject
                            showSubjectMenu = false
                        }
                    )
                }
            }
        }

        // Semester dropdown
        ExposedDropdownMenuBox(
            expanded = showSemesterMenu,
            onExpandedChange = {
                if (uploadProgress !is UploadProgress.Uploading) {
                    showSemesterMenu = !showSemesterMenu
                }
            }
        ) {
            OutlinedTextField(
                value = selectedSemester,
                onValueChange = {},
                readOnly = true,
                label = { Text("Semester *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSemesterMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                enabled = uploadProgress !is UploadProgress.Uploading
            )

            ExposedDropdownMenu(
                expanded = showSemesterMenu,
                onDismissRequest = { showSemesterMenu = false }
            ) {
                Constants.SEMESTERS.forEach { semester ->
                    DropdownMenuItem(
                        text = { Text(semester) },
                        onClick = {
                            selectedSemester = semester
                            showSemesterMenu = false
                        }
                    )
                }
            }
        }

        // File picker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = uploadProgress !is UploadProgress.Uploading) {
                    filePicker.launch("*/*")
                },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedFileUri != null)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (selectedFileUri != null) Icons.Default.CheckCircle else Icons.Default.Add,
                    contentDescription = "Upload file",
                    modifier = Modifier.size(48.dp),
                    tint = if (selectedFileUri != null)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedFileUri != null) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = FileUtils.formatFileSize(fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Select File",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "PDF Files Only (Max ${Constants.MAX_FILE_SIZE_MB}MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Progress indicator
        when (val progress = uploadProgress) {
            is UploadProgress.Uploading -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Uploading... ${progress.progress}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is UploadProgress.Validating -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Validating file...")
                }
            }
            is UploadProgress.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = progress.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            else -> {}
        }

        Spacer(modifier = Modifier.weight(1f))

        // Upload button
        Button(
            onClick = {
                viewModel.uploadNote(
                    context = context,
                    title = title,
                    description = description,
                    subject = selectedSubject,
                    semester = selectedSemester
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uploadProgress !is UploadProgress.Uploading &&
                    title.isNotBlank() &&
                    selectedSubject.isNotBlank() &&
                    selectedSemester.isNotBlank() &&
                    selectedFileUri != null
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Note")
        }
    }
}

@Composable
fun ErrorMessage(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Empty",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


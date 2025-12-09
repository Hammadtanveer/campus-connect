package com.example.campusconnect.ui.screens
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.data.models.UploadProgress
import com.example.campusconnect.ui.viewmodels.UploadNoteViewModel
import com.example.campusconnect.util.Constants
import com.example.campusconnect.util.FileUtils
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadNoteScreen(
    onNavigateBack: () -> Unit,
    onUploadSuccess: () -> Unit
) {
    val viewModel: UploadNoteViewModel = hiltViewModel()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val selectedFileUri by viewModel.selectedFileUri.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val fileSize by viewModel.fileSize.collectAsState()
    var currentStep by remember { mutableStateOf(1) }
    var selectedSemester by remember { mutableStateOf<String?>(null) }
    var selectedSubjectCode by remember { mutableStateOf<Constants.SubjectCode?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
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
            viewModel.resetUpload()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
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
            // Progress Stepper
            StepProgressIndicator(
                currentStep = currentStep,
                totalSteps = 4,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    when (currentStep) {
                        1 -> SemesterSelectionStep(
                            selectedSemester = selectedSemester,
                            onSemesterSelected = { 
                                selectedSemester = it
                                selectedSubjectCode = null
                            }
                        )
                        2 -> SubjectCodeSelectionStep(
                            semester = selectedSemester ?: "",
                            selectedSubjectCode = selectedSubjectCode,
                            onSubjectCodeSelected = { selectedSubjectCode = it }
                        )
                        3 -> FileSelectionStep(
                            selectedFileUri = selectedFileUri,
                            fileName = fileName,
                            fileSize = fileSize,
                            uploadProgress = uploadProgress,
                            onSelectFile = { filePicker.launch("application/pdf") },
                            onClearError = { viewModel.clearError() }
                        )
                        4 -> NotesDetailsStep(
                            title = title,
                            description = description,
                            onTitleChange = { title = it },
                            onDescriptionChange = { description = it },
                            uploadProgress = uploadProgress
                        )
                    }
                }
            }
            // Navigation Buttons
            NavigationButtons(
                currentStep = currentStep,
                totalSteps = 4,
                canProceed = when (currentStep) {
                    1 -> selectedSemester != null
                    2 -> selectedSubjectCode != null
                    3 -> selectedFileUri != null && uploadProgress !is UploadProgress.Error
                    4 -> title.isNotBlank()
                    else -> false
                },
                uploadProgress = uploadProgress,
                onNext = {
                    if (currentStep < 4) {
                        currentStep++
                    } else {
                        selectedSubjectCode?.let { subjectCode ->
                            viewModel.uploadNote(
                                context = context,
                                title = title,
                                description = description,
                                subject = "${subjectCode.code} - ${subjectCode.name}",
                                semester = subjectCode.semester
                            )
                        }
                    }
                },
                onBack = {
                    if (currentStep > 1) {
                        currentStep--
                    } else {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
@Composable
fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..totalSteps) {
                StepCircle(
                    stepNumber = step,
                    isCompleted = step < currentStep,
                    isCurrent = step == currentStep,
                    modifier = Modifier.weight(1f)
                )
                if (step < totalSteps) {
                    HorizontalDivider(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp),
                        color = if (step < currentStep) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (currentStep) {
                1 -> "Select Semester"
                2 -> "Choose Subject"
                3 -> "Upload File"
                4 -> "Add Details"
                else -> ""
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun StepCircle(
    stepNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.wrapContentWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCurrent)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
fun SemesterSelectionStep(
    selectedSemester: String?,
    onSemesterSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Which semester is this note for?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select the semester to view available subjects",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 500.dp)
        ) {
            items(Constants.SEMESTERS) { semester ->
                SemesterCard(
                    semester = semester,
                    isSelected = semester == selectedSemester,
                    onClick = { onSemesterSelected(semester) }
                )
            }
        }
    }
}
@Composable
fun SemesterCard(
    semester: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = semester,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun SubjectCodeSelectionStep(
    semester: String,
    selectedSubjectCode: Constants.SubjectCode?,
    onSubjectCodeSelected: (Constants.SubjectCode) -> Unit
) {
    val subjectCodes = Constants.getSubjectCodesForSemester(semester)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Select Subject Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose the subject for your note from $semester",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subjectCodes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "No subjects available for this semester",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            subjectCodes.forEach { subjectCode ->
                SubjectCodeCard(
                    subjectCode = subjectCode,
                    isSelected = subjectCode == selectedSubjectCode,
                    onClick = { onSubjectCodeSelected(subjectCode) }
                )
            }
        }
    }
}
@Composable
fun SubjectCodeCard(
    subjectCode: Constants.SubjectCode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectCode.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subjectCode.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
@Composable
fun FileSelectionStep(
    selectedFileUri: Uri?,
    fileName: String,
    fileSize: Long,
    uploadProgress: UploadProgress,
    onSelectFile: () -> Unit,
    onClearError: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Upload Your Note",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select a PDF file (Max ${Constants.MAX_FILE_SIZE_MB}MB)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            onClick = onSelectFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedFileUri != null)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            enabled = uploadProgress !is UploadProgress.Uploading
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selectedFileUri != null) Icons.Default.Done else Icons.Default.Add,
                    contentDescription = "Upload",
                    modifier = Modifier.size(64.dp),
                    tint = if (selectedFileUri != null)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedFileUri != null) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = FileUtils.formatFileSize(fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Tap to Select PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "PDF files only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        when (uploadProgress) {
            is UploadProgress.Validating -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = uploadProgress.message,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = onClearError) {
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
    }
}
@Composable
fun NotesDetailsStep(
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    uploadProgress: UploadProgress
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Add Note Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Provide a title and optional description for your note",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title *") },
            placeholder = { Text("e.g., Chapter 5 - Sorting Algorithms") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = uploadProgress !is UploadProgress.Uploading,
            isError = title.isBlank(),
            supportingText = if (title.isBlank()) {
                { Text("Title is required") }
            } else null
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (optional)") },
            placeholder = { Text("Add additional context about this note...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6,
            enabled = uploadProgress !is UploadProgress.Uploading
        )
        AnimatedVisibility(
            visible = uploadProgress is UploadProgress.Uploading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (uploadProgress is UploadProgress.Uploading) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(
                        progress = { uploadProgress.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Uploading... ${uploadProgress.progress}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
@Composable
fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canProceed: Boolean,
    uploadProgress: UploadProgress,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f),
            enabled = uploadProgress !is UploadProgress.Uploading
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (currentStep == 1) "Cancel" else "Back")
        }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f),
            enabled = canProceed && uploadProgress !is UploadProgress.Uploading
        ) {
            Text(if (currentStep == totalSteps) "Upload" else "Next")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                if (currentStep == totalSteps) Icons.Default.Send else Icons.Default.ArrowForward,
                contentDescription = null
            )
        }
    }
}











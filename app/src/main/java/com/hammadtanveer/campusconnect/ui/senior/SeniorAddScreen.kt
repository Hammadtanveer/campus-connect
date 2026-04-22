package com.hammadtanveer.campusconnect.ui.senior

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hammadtanveer.campusconnect.MainViewModel
import com.hammadtanveer.campusconnect.data.models.SeniorProfile
import com.hammadtanveer.campusconnect.ui.components.AppOverflowMenu
import com.hammadtanveer.campusconnect.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorAddScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    // 1. State variables start empty for a new entry
    var name by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf("") }
    var branchError by remember { mutableStateOf("") }
    var batchError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var mobileError by remember { mutableStateOf("") }
    var linkedinError by remember { mutableStateOf("") }
    
    var isUploading by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "Name is required"
            isValid = false
        } else nameError = ""

        if (branch.isBlank()) {
            branchError = "Branch is required"
            isValid = false
        } else branchError = ""

        if (batch.isBlank() || batch.length != 4) {
            batchError = "Enter valid 4-digit batch year"
            isValid = false
        } else batchError = ""

        if (email.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter valid email address"
            isValid = false
        } else emailError = ""

        if (mobileNumber.isBlank()) {
            mobileError = "Mobile number is required"
            isValid = false
        } else if (mobileNumber.length != 10) {
            mobileError = "Enter valid 10-digit mobile number"
            isValid = false
        } else mobileError = ""

        if (linkedin.isNotBlank() &&
            !linkedin.startsWith("https://www.linkedin.com") &&
            !linkedin.startsWith("https://linkedin.com")
        ) {
            linkedinError = "Enter valid LinkedIn URL"
            isValid = false
        } else linkedinError = ""

        return isValid
    }

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
                    if (url != null && url.isNotBlank()) {
                        // Force HTTPS - replace http:// with https://
                        profileImageUrl = url.replace("http://", "https://")
                        android.util.Log.d("ImageUpload", 
                            "Image uploaded successfully: $profileImageUrl")
                    } else {
                        android.util.Log.e("ImageUpload", 
                            "Upload returned null or empty URL")
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
                },
                actions = {
                    AppOverflowMenu(
                        userProfile = viewModel.userProfile,
                        onOpenAdminPanel = onOpenAdminPanel,
                        onLogout = { viewModel.signOut() }
                    )
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
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { 
                        launcher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        ) 
                    }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Add Photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Edit overlay icon
                if (profileImageUrl.isNotBlank() && !isUploading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.all { c -> c.isLetter() || c.isWhitespace() }) name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError.isNotEmpty(),
                supportingText = { if (nameError.isNotEmpty()) Text(nameError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it },
                label = { Text("Branch") },
                modifier = Modifier.fillMaxWidth(),
                isError = branchError.isNotEmpty(),
                supportingText = { if (branchError.isNotEmpty()) Text(branchError, color = MaterialTheme.colorScheme.error) }
            )
            OutlinedTextField(
                value = batch,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) batch = it },
                label = { Text("Batch") },
                modifier = Modifier.fillMaxWidth(),
                isError = batchError.isNotEmpty(),
                supportingText = { if (batchError.isNotEmpty()) Text(batchError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = emailError.isNotEmpty(),
                supportingText = { if (emailError.isNotEmpty()) Text(emailError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 10) mobileNumber = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth(),
                isError = mobileError.isNotEmpty(),
                supportingText = { if (mobileError.isNotEmpty()) Text(mobileError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                label = { Text("Company Placed") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = linkedin,
                onValueChange = { linkedin = it },
                label = { Text("LinkedIn URL") },
                modifier = Modifier.fillMaxWidth(),
                isError = linkedinError.isNotEmpty(),
                supportingText = { if (linkedinError.isNotEmpty()) Text(linkedinError, color = MaterialTheme.colorScheme.error) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, showKeyboardOnFocus = true),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. The Add Button
            Button(
                onClick = {
                    if (validate()) {
                        isSubmitting = true
                        val senior = SeniorProfile(
                            name = name.trim(),
                            branch = branch.trim(),
                            batch = batch.trim(),
                            companyPlaced = company.trim(),
                            linkedinUrl = linkedin.trim(),
                            bio = bio.trim(),
                            email = email.trim(),
                            mobileNumber = mobileNumber.trim(),
                            profileImageUrl = profileImageUrl
                        )
                        viewModel.addSenior(senior) { success, error ->
                            isSubmitting = false
                            if (success) {
                                navController.popBackStack()
                            } else {
                                android.widget.Toast.makeText(context, "Error: $error", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Add Senior")
                }
            }
        }
    }
}

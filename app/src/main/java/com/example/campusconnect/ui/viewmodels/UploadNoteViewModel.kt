package com.example.campusconnect.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.models.UploadProgress
import com.example.campusconnect.data.repository.NotesRepository
import com.example.campusconnect.util.FileUtils
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for note upload functionality.
 *
 * Uses Hilt for dependency injection of repository and auth.
 */
@HiltViewModel
class UploadNoteViewModel @Inject constructor(
    private val repository: NotesRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val MAX_UPLOAD_FILE_BYTES = 20L * 1024L * 1024L
    }

    private val _uploadProgress = MutableStateFlow<UploadProgress>(UploadProgress.Idle)
    val uploadProgress: StateFlow<UploadProgress> = _uploadProgress.asStateFlow()

    private val _selectedFileUri = MutableStateFlow<Uri?>(null)
    val selectedFileUri: StateFlow<Uri?> = _selectedFileUri.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    private val _fileSize = MutableStateFlow(0L)
    val fileSize: StateFlow<Long> = _fileSize.asStateFlow()

    fun selectFile(context: Context, uri: Uri) {
        _uploadProgress.value = UploadProgress.Validating

        val selectedName = FileUtils.getFileName(context, uri)
        val selectedSize = FileUtils.getFileSize(context, uri)
        val mimeType = FileUtils.getMimeType(context, uri)

        when {
            selectedSize <= 0L -> {
                _uploadProgress.value = UploadProgress.Error("File not found or empty")
            }
            selectedSize > MAX_UPLOAD_FILE_BYTES -> {
                _uploadProgress.value = UploadProgress.Error("File size must be 20MB or less")
            }
            !FileUtils.isFileTypeValid(selectedName) -> {
                _uploadProgress.value = UploadProgress.Error("Only PDF files are allowed")
            }
            mimeType != null && mimeType != "application/pdf" -> {
                _uploadProgress.value = UploadProgress.Error("Invalid file format. Only PDF files are supported.")
            }
            else -> {
                _selectedFileUri.value = uri
                _fileName.value = selectedName
                _fileSize.value = selectedSize
                _uploadProgress.value = UploadProgress.Idle
            }
        }
    }

    fun uploadNote(
        context: Context,
        title: String,
        description: String,
        subjectCode: String,
        subjectName: String,
        branch: String,
        semester: String
    ) {
        val uri = _selectedFileUri.value
        if (uri == null) {
            _uploadProgress.value = UploadProgress.Error("Please select a PDF file")
            return
        }

        val user = auth.currentUser
        if (user == null) {
            _uploadProgress.value = UploadProgress.Error("User not authenticated")
            return
        }

        if (title.isBlank() || subjectCode.isBlank() || subjectName.isBlank() || branch.isBlank() || semester.isBlank()) {
            _uploadProgress.value = UploadProgress.Error("Please fill all required fields")
            return
        }

        viewModelScope.launch {
            try {
                _uploadProgress.value = UploadProgress.Uploading(0)

                val selectedName = FileUtils.getFileName(context, uri)
                val cacheFile = FileUtils.copyFileToCache(context, uri, selectedName)

                if (cacheFile == null) {
                    _uploadProgress.value = UploadProgress.Error("Failed to process file")
                    return@launch
                }

                val subject = "${subjectCode.trim()} - ${subjectName.trim()} (${branch.trim()})"
                val result = repository.uploadNote(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject,
                    semester = semester,
                    file = cacheFile,
                    userId = user.uid,
                    userName = user.displayName ?: user.email ?: "Anonymous",
                    onProgress = { progress ->
                        _uploadProgress.value = UploadProgress.Uploading(progress)
                    }
                )

                cacheFile.delete()

                when (result) {
                    is Resource.Success -> _uploadProgress.value = UploadProgress.Success(result.data ?: "")
                    is Resource.Error -> _uploadProgress.value = UploadProgress.Error(result.message ?: "Upload failed")
                    else -> _uploadProgress.value = UploadProgress.Error("Unknown error occurred")
                }
            } catch (e: Exception) {
                _uploadProgress.value = UploadProgress.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun resetUpload() {
        _uploadProgress.value = UploadProgress.Idle
        _selectedFileUri.value = null
        _fileName.value = ""
        _fileSize.value = 0L
    }

    fun clearError() {
        if (_uploadProgress.value is UploadProgress.Error) {
            _uploadProgress.value = UploadProgress.Idle
        }
    }
}

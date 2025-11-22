package com.example.campusconnect.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

    /**
     * Get file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            return cursor.getLong(sizeIndex)
        }
        return 0L
    }

    /**
     * Get file name from URI
     */
    fun getFileName(context: Context, uri: Uri): String {
        var fileName = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }
        return fileName
    }

    /**
     * Get file extension
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    /**
     * Get file type category - PDF ONLY
     */
    fun getFileType(fileName: String): String {
        return when (getFileExtension(fileName).lowercase()) {
            "pdf" -> "pdf"
            else -> "other"
        }
    }

    /**
     * Get MIME type from URI
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri) ?: run {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        }
    }

    /**
     * Format file size to human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

        return String.format(
            "%.1f %s",
            bytes / 1024.0.pow(digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * Validate file size
     */
    fun isFileSizeValid(bytes: Long, maxBytes: Long = Constants.MAX_FILE_SIZE_BYTES): Boolean {
        return bytes in 1..maxBytes
    }

    /**
     * Validate file type - PDF ONLY
     */
    fun isFileTypeValid(fileName: String): Boolean {
        val extension = getFileExtension(fileName)
        return extension in Constants.ALLOWED_FILE_TYPES
    }

    /**
     * Copy file from URI to cache directory
     */
    fun copyFileToCache(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Validate file for upload - PDF ONLY
     */
    fun validateFile(context: Context, uri: Uri): FileValidationResult {
        val fileName = getFileName(context, uri)
        val fileSize = getFileSize(context, uri)
        val mimeType = getMimeType(context, uri)

        // Check if file exists
        if (fileSize == 0L) {
            return FileValidationResult(false, "File not found or is empty")
        }

        // Check file size
        if (!isFileSizeValid(fileSize)) {
            return FileValidationResult(
                false,
                "File size must be less than ${formatFileSize(Constants.MAX_FILE_SIZE_BYTES)}"
            )
        }

        // Check file type - PDF ONLY
        if (!isFileTypeValid(fileName)) {
            return FileValidationResult(
                false,
                "Only PDF files are allowed"
            )
        }

        // Check MIME type - PDF ONLY
        if (mimeType != null && mimeType !in Constants.ALLOWED_MIME_TYPES) {
            return FileValidationResult(
                false,
                "Invalid file format. Only PDF files are supported."
            )
        }

        return FileValidationResult(true, "File is valid")
    }
}

data class FileValidationResult(
    val isValid: Boolean,
    val message: String
)


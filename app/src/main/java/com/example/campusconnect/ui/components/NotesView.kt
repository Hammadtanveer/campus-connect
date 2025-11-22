package com.example.campusconnect.ui.components

import com.example.campusconnect.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.campusconnect.MainViewModel
import com.example.campusconnect.security.canUploadNotes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import com.example.campusconnect.data.models.Note


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Notes(viewModel: MainViewModel? = null){
    // Define different data per year
    val sections: List<Pair<String, List<String>>> = listOf(
        "8th Sem" to listOf("BCS-801", "BCS-802","BCS-851","MNPM-801"),
        "7th Sem" to listOf("BCS-701", "BCS-702","BCS-751","BCS-752","BCS-753", "HTCS-701"),
        "6th Sem" to listOf("BCS-601","BCS-602","BCS-651","BCS-652","BOE-060"),
        "5th Sem" to listOf("BCS-501","BCS-502","BCS-503","BCS-052","BCS-055"),
        "4th Sem" to listOf("BCS-401","BCS-402","BCS-403","BCC-402","BAS403","BVE-401"),
        "3rd Sem" to listOf("BCS-301","BCS-302","BCS-303","BCC-301","BOE-310","BAS-301"),
        "2nd Sem" to listOf("BAS-201","BAS-203","BEE-201","BAS204"),
        "1st Sem" to listOf("BAS-101","BAS-103","BEC-101","BAS-105")
    )

    val showUploadResult = remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uploadState = remember { mutableStateOf<String?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && viewModel != null) {
            val bytes = readAllBytes(context, uri)
            if (bytes != null) {
                viewModel.uploadPdfNote(title = uri.lastPathSegment ?: "note.pdf", pdfBytes = bytes) { ok, err ->
                    uploadState.value = if (ok) "Uploaded" else err
                }
            } else uploadState.value = "Failed to read file"
        }
    }
    LaunchedEffect(viewModel?.userProfile?.id) { viewModel?.observeMyNotes() }
    LazyColumn {
        item {
            if (viewModel?.userProfile?.canUploadNotes() == true) {
                Button(onClick = { pdfPicker.launch("application/pdf") }) { Text("Select PDF & Upload") }
                uploadState.value?.let { msg -> Text(msg ?: "", color = MaterialTheme.colorScheme.primary) }
            }
        }
        // Show list of uploaded notes
        item {
            val notes = viewModel?.myNotes ?: emptyList()
            if (notes.isNotEmpty()) {
                Text("My Notes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
                notes.forEach { n ->
                    Text("• ${n.title} (${n.fileSize/1024}KB)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        sections.forEach { (sem, courses) ->
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sem,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(courses) { cat ->
                        BrowserItem(cat = cat, drawable = R.drawable.outline_book_2_24)
                    }
                }
            }
        }
    }
}
@Composable
fun BrowserItem(cat:String, drawable:Int){
    Card(modifier = Modifier.padding(16.dp).size(200.dp),
        border = BorderStroke(3.dp, color = MaterialTheme.colorScheme.outline),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ){
        Column (
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ){
            Text(text = cat, color = MaterialTheme.colorScheme.onSurface)
            Image(painter = painterResource(id = drawable), contentDescription = cat)
        }
    }
}

private fun readAllBytes(context: Context, uri: Uri): ByteArray? = try {
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
} catch (_: Exception) { null }

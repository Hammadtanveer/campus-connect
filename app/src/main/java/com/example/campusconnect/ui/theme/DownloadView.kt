package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.campusconnect.R
import com.example.campusconnect.MainViewModel

@Composable
fun DownloadView(viewModel: MainViewModel){

    val downloads = viewModel.downloads.value

    Column (
        modifier = Modifier.height(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "Download", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ){
            Column ( modifier= Modifier.padding(12.dp)){
                 Column {
                     Text(text = "Offline notes", style = MaterialTheme.typography.titleSmall)
                     Row (
                         modifier = Modifier.fillMaxWidth(),
                         horizontalArrangement = Arrangement.SpaceBetween
                     ){
                         val subtitle = if (downloads.isEmpty()) "No downloads yet" else "${downloads.size} saved"
                         Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                         TextButton(onClick = { viewModel.clearDownloads() }) {
                             Text(text = "Manage")
                             Icon(
                                 imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                 contentDescription = "Manage"
                             )
                         }

                     }
                 }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                if (downloads.isEmpty()){
                    Row (Modifier.padding(vertical = 8.dp)){
                        Icon(
                            painter = painterResource(id = R.drawable.outline_book_2_24),
                            contentDescription = "Downloads"
                        )
                        Text(
                            text ="Saved items will appear here",
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.addDownload(title = "Sample Note.pdf", sizeLabel = "1.2 MB") }){
                        Text("Add sample")
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)){
                        items(downloads, key = { it.id }){ item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Row(verticalAlignment = Alignment.CenterVertically){
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_book_2_24),
                                        contentDescription = null
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)){
                                        Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
                                        Text(text = item.sizeLabel, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TextButton(onClick = { viewModel.removeDownload(item.id) }){
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
package com.example.campusconnect


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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Notes(){
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

    LazyColumn {
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
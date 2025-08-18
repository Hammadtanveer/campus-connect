package com.example.campusconnect.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Seniors() {
    val names = listOf(
        "Hammad Tanveer",
        "Mohd Faisal",
        "Mohammad Adnan",
        "Mohd Arham",
        "Harsh Kumar"
    )

    LazyColumn {
        items(names.size) { index ->
            LibItem(name = names[index])
        }
    }
}

@Composable
fun LibItem(name: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = name,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(text = name)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Arrow Right"
            )
        }
        Divider(color = Color.LightGray)
    }
}
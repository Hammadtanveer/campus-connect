package com.example.campusconnect

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.grid.GridCells

@Composable
fun Societies (){
    val categories = listOf("MIT Literary Society","MIT Tech Club","Hobbies Club","CSSS","MITSA","MESS")
    LazyVerticalGrid(GridCells.Fixed(2)) {
        items(categories){
                cat ->
            BrowserItem(cat = cat, drawable = R.drawable.outline_person_play_24)
        }
    }
}
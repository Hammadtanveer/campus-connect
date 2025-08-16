package com.example.campusconnect

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.util.UUID

class MainViewModel : ViewModel() {


    private val _currentScreen: MutableState<Screen> = mutableStateOf(Screen.DrawerScreen.AddAccount)

    val currentScreen : MutableState<Screen>
        get() = _currentScreen

    fun setCurrentScreen(screen: Screen){
        _currentScreen.value = screen
    }

    // Simple downloads state for the Downloads screen
    data class DownloadItem(val id: String = UUID.randomUUID().toString(), val title: String, val sizeLabel: String)

    private val _downloads: MutableState<List<DownloadItem>> = mutableStateOf(emptyList())
    val downloads: MutableState<List<DownloadItem>>
        get() = _downloads

    fun addDownload(title: String, sizeLabel: String) {
        _downloads.value = _downloads.value + DownloadItem(title = title, sizeLabel = sizeLabel)
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filterNot { it.id == id }
    }

    fun clearDownloads() {
        _downloads.value = emptyList()
    }
}
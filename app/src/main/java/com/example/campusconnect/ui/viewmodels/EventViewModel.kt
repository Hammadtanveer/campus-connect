package com.example.campusconnect.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusconnect.data.models.SocietyEvent
import com.example.campusconnect.data.models.Resource
import com.example.campusconnect.data.repository.SocietyEventRepository
import com.example.campusconnect.ui.state.UiState.Loading.isLoading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: SocietyEventRepository
) : ViewModel() {

    var addEventStatus by mutableStateOf<Resource<String>?>(null)
        private set

    var selectedEvent by mutableStateOf<SocietyEvent?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set
    var loding by mutableStateOf(false)
        private set

    fun getSocietyEvent(eventId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.getEventById(eventId)) {
                is Resource.Success -> {
                    selectedEvent = result.data
                }
                is Resource.Error -> {
                    errorMessage = result.message
                }
                is Resource.Loading -> {
                    // Handled by isLoading
                }
            }
            isLoading = false
        }
    }

    fun createEvent(
//        societyName: String,
        name: String,
        date: String,
        time: String,
        venue: String,
        coordinator: String,
        convener: String,
        register: String
    ) {
        viewModelScope.launch {
            addEventStatus = Resource.Loading
            val event = SocietyEvent(
//                societyName = societyName,
                name = name,
                date = date,
                time = time,
                venue = venue,
                coordinator = coordinator,
                convener = convener,
                register = register
            )
            addEventStatus = repository.addEvent(event)
        }
    }

    fun resetStatus() {
        addEventStatus = null
    }
}

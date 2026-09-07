package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.services.SafePlacesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafePlacesViewModel @Inject constructor(
    private val safePlacesService: SafePlacesService
) : ViewModel() {

    data class UiState(
        val nearbyPlaces: List<SafePlace> = emptyList(),
        val userPlaces: List<SafePlace> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val searchRadius: Double = 5000.0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadNearbyPlaces(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val places = safePlacesService.nearby(latitude, longitude, _uiState.value.searchRadius)
            _uiState.value = _uiState.value.copy(
                nearbyPlaces = places,
                isLoading = false
            )
        }
    }

    fun loadUserPlaces() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allPlaces = safePlacesService.all()
            val userPlaces = allPlaces.filter { it.type == "user" }
            _uiState.value = _uiState.value.copy(
                userPlaces = userPlaces,
                isLoading = false
            )
        }
    }

    fun addPlace(name: String, address: String, latitude: Double, longitude: Double, type: String = "user") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val place = safePlacesService.addPlace(name, address, latitude, longitude, type)
            _uiState.value = _uiState.value.copy(
                userPlaces = _uiState.value.userPlaces + place,
                isLoading = false
            )
        }
    }

    fun removePlace(id: String) {
        viewModelScope.launch {
            safePlacesService.removePlace(id)
            _uiState.value = _uiState.value.copy(
                userPlaces = _uiState.value.userPlaces.filterNot { it.id == id }
            )
        }
    }

    fun setSearchRadius(radius: Double) {
        _uiState.value = _uiState.value.copy(searchRadius = radius)
    }
}
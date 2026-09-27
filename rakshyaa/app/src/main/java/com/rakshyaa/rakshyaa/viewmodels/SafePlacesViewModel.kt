package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.data.repositories.LocationRepository
import com.rakshyaa.rakshyaa.services.SafePlacesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafePlacesViewModel @Inject constructor(
    private val safePlacesService: SafePlacesService,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val nearbyPlaces: List<SafePlace> = emptyList(),
        val userPlaces: List<SafePlace> = emptyList(),
        val closestPlace: SafePlace? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val searchRadius: Double = 5000.0,
        val currentLatitude: Double = 27.7172,
        val currentLongitude: Double = 85.3240,
        val locationUnavailable: Boolean = false,
        val isLive: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadWithCurrentLocation()
    }

    private companion object {
        const val FALLBACK_LAT = 27.7172
        const val FALLBACK_LON = 85.3240
    }

    fun loadWithCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val loc = locationRepository.currentLocation()
            if (loc == null) {
                val fallback = safePlacesService.offlineFallback(FALLBACK_LAT, FALLBACK_LON, _uiState.value.searchRadius)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    locationUnavailable = true,
                    nearbyPlaces = fallback.nearby,
                    closestPlace = fallback.closest,
                    isLive = false
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                currentLatitude = loc.latitude,
                currentLongitude = loc.longitude,
                locationUnavailable = false
            )
            val result = safePlacesService.nearby(loc.latitude, loc.longitude, _uiState.value.searchRadius)
            _uiState.value = _uiState.value.copy(
                nearbyPlaces = result.nearby,
                closestPlace = result.closest,
                isLoading = false,
                isLive = result.isLive
            )
            loadUserPlaces()
        }
    }

    fun loadNearbyPlaces(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentLatitude = latitude,
                currentLongitude = longitude,
                locationUnavailable = false
            )
            val result = safePlacesService.nearby(latitude, longitude, _uiState.value.searchRadius)
            _uiState.value = _uiState.value.copy(
                nearbyPlaces = result.nearby,
                closestPlace = result.closest,
                isLoading = false,
                isLive = result.isLive
            )
        }
    }

    fun loadUserPlaces() {
        viewModelScope.launch {
            val allPlaces = safePlacesService.all()
            val userPlaces = allPlaces.filter { it.type == "user" }
            _uiState.value = _uiState.value.copy(userPlaces = userPlaces)
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
        loadWithCurrentLocation()
    }
}
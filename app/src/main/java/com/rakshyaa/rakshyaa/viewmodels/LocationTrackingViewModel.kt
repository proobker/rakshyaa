package com.rakshyaa.rakshyaa.viewmodels

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.LocationRecord
import com.rakshyaa.rakshyaa.data.repositories.LocationRepository
import com.rakshyaa.rakshyaa.services.LocationTrackingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationTrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository
) : ViewModel() {

    data class UiState(
        val isTracking: Boolean = false,
        val lastLocation: LocationRecord? = null,
        val locationHistory: List<LocationRecord> = emptyList(),
        val hasFineLocationPermission: Boolean = false,
        val hasBackgroundLocationPermission: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
            val last = locationRepository.getLastKnownLocation()
            val history = locationRepository.getLocationHistory()
            _uiState.value = _uiState.value.copy(
                hasFineLocationPermission = fine,
                hasBackgroundLocationPermission = bg,
                lastLocation = last,
                locationHistory = history,
                isLoading = false
            )
        }
    }

    fun requestPermissions(activity: androidx.activity.ComponentActivity, launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        launcher.launch(perms)
    }

    fun onPermissionResult(granted: BooleanArray) {
        val fine = granted.firstOrNull() == true
        val bg = if (granted.size > 1) granted[1] == true else true
        _uiState.value = _uiState.value.copy(
            hasFineLocationPermission = fine,
            hasBackgroundLocationPermission = bg
        )
        if (fine && bg) {
            startTracking()
        }
    }

    fun startTracking() {
        _uiState.value = _uiState.value.copy(isTracking = true)
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_LOCATION_UPDATES
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking() {
        _uiState.value = _uiState.value.copy(isTracking = false)
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_LOCATION_UPDATES
        }
        context.startService(intent)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
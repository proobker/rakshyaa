package com.rakshyaa.rakshyaa.viewmodels

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.RideSession
import com.rakshyaa.rakshyaa.data.repositories.RideRepository
import com.rakshyaa.rakshyaa.services.RideMonitoringService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RideMonitoringViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rideRepository: RideRepository
) : ViewModel() {

    data class UiState(
        val activeRide: RideSession? = null,
        val rideHistory: List<RideSession> = emptyList(),
        val isLoading: Boolean = false,
        val isMonitoring: Boolean = false,
        val hasLocationPermission: Boolean = false,
        val error: String? = null,
        val deviationThreshold: Double = 50.0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        updateLocationPermission(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
        loadRides()
    }

    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
    }

    fun loadRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val active = rideRepository.getActive()
            val history = rideRepository.getAll()
            _uiState.value = _uiState.value.copy(
                activeRide = active,
                rideHistory = history,
                isLoading = false,
                isMonitoring = active != null
            )
        }
    }

    fun startRide(threshold: Double = 50.0) {
        if (!_uiState.value.hasLocationPermission) {
            _uiState.value = _uiState.value.copy(
                error = "Location permission is required to monitor a ride"
            )
            return
        }
        val intent = Intent(context, RideMonitoringService::class.java).apply {
            action = RideMonitoringService.ACTION_START_RIDE_MONITORING
            putExtra(RideMonitoringService.EXTRA_DEVIATION_THRESHOLD_M, threshold)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        _uiState.value = _uiState.value.copy(deviationThreshold = threshold, isMonitoring = true, error = null)
    }

    fun stopRide() {
        val intent = Intent(context, RideMonitoringService::class.java).apply {
            action = RideMonitoringService.ACTION_STOP_RIDE_MONITORING
        }
        runCatching { context.startService(intent) }
        _uiState.value = _uiState.value.copy(isMonitoring = false)
        loadRides()
    }

    fun updateDeviationThreshold(threshold: Double) {
        _uiState.value = _uiState.value.copy(deviationThreshold = threshold)
        val intent = Intent(context, RideMonitoringService::class.java).apply {
            action = RideMonitoringService.ACTION_UPDATE_ROUTE
            putExtra(RideMonitoringService.EXTRA_DEVIATION_THRESHOLD_M, threshold)
        }
        runCatching { context.startService(intent) }
    }
}

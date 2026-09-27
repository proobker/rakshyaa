package com.rakshyaa.rakshyaa.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.services.SOSActivationService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the SOS activation flow: runs the countdown before genuinely
 * starting the [SOSActivationService] foreground service.
 */
@HiltViewModel
class SOSViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class UiState(
        val isSosActivating: Boolean = false,
        val isSosActive: Boolean = false,
        val sosActivationCountdown: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            com.rakshyaa.rakshyaa.services.SosRuntime.state.collect { state ->
                _uiState.update { it.copy(isSosActive = state.active, error = state.error) }
            }
        }
    }

    private var countdownJob: Job? = null

    fun activateSos() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSosActivating = true, isSosActive = false, sosActivationCountdown = 5)
            }
            countdownJob?.cancel()
            countdownJob = launch {
                for (i in 4 downTo 0) {
                    delay(1000)
                    _uiState.update { state -> state.copy(sosActivationCountdown = i) }
                }
                startSosService(isFalseAlarm = false)
                _uiState.update {
                    it.copy(isSosActivating = false, sosActivationCountdown = 0)
                }
            }
        }
    }

    fun deactivateSos() {
        viewModelScope.launch {
            countdownJob?.cancel()
            stopSosService()
            _uiState.update {
                it.copy(isSosActivating = false, sosActivationCountdown = 0)
            }
        }
    }

    fun cancelSosActivation() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(isSosActivating = false, isSosActive = false, sosActivationCountdown = 0)
        }
    }

    private fun startSosService(isFalseAlarm: Boolean) {
        val intent = Intent(context, SOSActivationService::class.java).apply {
            action = SOSActivationService.ACTION_ACTIVATE_SOS
            putExtra(SOSActivationService.EXTRA_IS_FALSE_ALARM, isFalseAlarm)
        }
        runCatching { androidx.core.content.ContextCompat.startForegroundService(context, intent) }
            .onFailure { com.rakshyaa.rakshyaa.services.SosRuntime.failed("SOS could not start. Use the message or dialer action.") }
    }

    private fun stopSosService() {
        val intent = Intent(context, SOSActivationService::class.java).apply {
            action = SOSActivationService.ACTION_DEACTIVATE_SOS
        }
        runCatching { context.startService(intent) }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}

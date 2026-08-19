package com.rakshyaa.rakshyaa.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.services.SOSActivationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.timer
import timber.log.Timber

/**
 * ViewModel for handling SOS activation UI state
 */
@HiltViewModel
class SOSViewModel @Inject constructor(
    private val sosActivationService: SOSActivationService
) : ViewModel() {

    // UI State
    data class UiState(
        val isSosActivating: Boolean = false,
        val isSosActive: Boolean = false,
        val sosActivationCountdown: Int = 0,
        val isLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Countdown job for activation timer
    private var countdownJob: Job? = null

    init {
        // Initialize UI state based on service state (if needed)
        // Note: SOSActivationService doesn't currently expose state as flows,
        // so we'll manage UI state through ViewModel methods
    }

    /**
     * Activate SOS - starts the 5-second countdown
     */
    fun activateSos() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isSosActivating = true,
                isSosActive = false,
                sosActivationCountdown = 5,
                isLoading = true
            )}

            // Cancel any existing countdown
            countdownJob?.cancel()

            // Start 5-second countdown
            countdownJob = launch {
                for (i in 4 downTo 0) {
                    delay(1000) // 1 second delay
                    _uiState.update { it.copy(
                        sosActivationCountdown = i
                    )}
                }
                // Countdown finished - activate SOS
                _uiState.update { it.copy(
                    isSosActivating = false,
                    isSosActive = true,
                    sosActivationCountdown = 0,
                    isLoading = false
                )}
                // Actually activate the SOS service
                sosActivationService.activateSos(false) // false = not a false alarm
            }
        }
    }

    /**
     * Deactivate SOS
     */
    fun deactivateSos() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true
            )}

            // Cancel any ongoing countdown
            countdownJob?.cancel()

            // Update UI state
            _uiState.update { it.copy(
                isSosActivating = false,
                isSosActive = false,
                sosActivationCountdown = 0,
                isLoading = false
            )}

            // Actually deactivate the SOS service
            sosActivationService.deactivateSos()
        }
    }

    /**
     * Cancel SOS activation during countdown
     */
    fun cancelSosActivation() {
        viewModelScope.launch {
            // Cancel the countdown
            countdownJob?.cancel()

            // Reset UI state
            _uiState.update { it.copy(
                isSosActivating = false,
                isSosActive = false,
                sosActivationCountdown = 0,
                isLoading = false
            )}
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any ongoing coroutines
        countdownJob?.cancel()
    }

    companion object {
        /** Factory for creating SOSViewModel instances */
        @Singleton
        class Factory @Inject constructor(
            private val sosActivationService: SOSActivationService
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SOSViewModel(sosActivationService) as T
            }
        }
    }
}
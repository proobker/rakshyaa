package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.services.FakeCallService
import com.rakshyaa.rakshyaa.services.OngoingFakeCall
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FakeCallPhase { IDLE, COUNTDOWN, INCOMING, CONNECTED }

@HiltViewModel
class FakeCallViewModel @Inject constructor(
    private val fakeCallService: FakeCallService
) : ViewModel() {

    data class UiState(
        val callerName: String = "Mom",
        val callerNumber: String = "+1-555-0123",
        val isVideoCall: Boolean = false,
        val triggerDelaySeconds: Int = 5,
        val phase: FakeCallPhase = FakeCallPhase.IDLE,
        val countdownRemaining: Int = 5,
        val ongoingCall: OngoingFakeCall? = null,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun updateCallerName(name: String) {
        _uiState.value = _uiState.value.copy(callerName = name)
    }

    fun updateCallerNumber(number: String) {
        _uiState.value = _uiState.value.copy(callerNumber = number)
    }

    fun toggleVideoCall(isVideo: Boolean) {
        _uiState.value = _uiState.value.copy(isVideoCall = isVideo)
    }

    fun setTriggerDelay(seconds: Int) {
        _uiState.value = _uiState.value.copy(triggerDelaySeconds = seconds.coerceIn(1, 300))
    }

    /**
     * Starts a countdown honoring the currently-selected live slider value.
     * Once the countdown finishes the fake call begins and the ringtone plays.
     */
    fun startCountdown() {
        countdownJob?.cancel()
        val delaySeconds = _uiState.value.triggerDelaySeconds.coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(
            phase = FakeCallPhase.COUNTDOWN,
            countdownRemaining = delaySeconds,
            error = null
        )
        countdownJob = viewModelScope.launch {
            while (_uiState.value.countdownRemaining > 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(countdownRemaining = _uiState.value.countdownRemaining - 1)
            }
            beginCall()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(phase = FakeCallPhase.IDLE)
    }

    private fun beginCall() {
        val current = _uiState.value
        try {
            val call = fakeCallService.startCall(
                callerName = current.callerName,
                callerNumber = current.callerNumber,
                isVideo = current.isVideoCall
            )
            fakeCallService.startRingtone()
            _uiState.value = _uiState.value.copy(
                ongoingCall = call,
                phase = FakeCallPhase.INCOMING
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                phase = FakeCallPhase.IDLE,
                error = e.message ?: "Failed to start fake call"
            )
        }
    }

    fun answerCall() {
        val answered = fakeCallService.answerCall()
        answered?.let {
            _uiState.value = _uiState.value.copy(ongoingCall = it, phase = FakeCallPhase.CONNECTED)
        }
    }

    fun endCall() {
        countdownJob?.cancel()
        fakeCallService.endCall()
        _uiState.value = _uiState.value.copy(
            ongoingCall = null,
            phase = FakeCallPhase.IDLE
        )
    }

    fun stopRingtone() {
        fakeCallService.stopRingtone()
    }
}
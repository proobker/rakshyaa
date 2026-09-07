package com.rakshyaa.rakshyaa.viewmodels

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.services.FakeCallService
import com.rakshyaa.rakshyaa.services.OngoingFakeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FakeCallViewModel @Inject constructor(
    private val fakeCallService: FakeCallService
) : ViewModel() {

    data class UiState(
        val callerName: String = "Mom",
        val callerNumber: String = "+1-555-0123",
        val isVideoCall: Boolean = false,
        val triggerDelaySeconds: Int = 5,
        val ongoingCall: OngoingFakeCall? = null,
        val isCallActive: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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

    fun triggerFakeCall() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.value = current.copy(error = null)
            try {
                val call = fakeCallService.startCall(
                    callerName = current.callerName,
                    callerNumber = current.callerNumber,
                    isVideo = current.isVideoCall
                )
                _uiState.value = _uiState.value.copy(
                    ongoingCall = call,
                    isCallActive = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to start fake call")
            }
        }
    }

    fun answerCall() {
        val answered = fakeCallService.answerCall()
        answered?.let {
            _uiState.value = _uiState.value.copy(ongoingCall = it)
        }
    }

    fun endCall() {
        fakeCallService.endCall()
        _uiState.value = _uiState.value.copy(
            ongoingCall = null,
            isCallActive = false
        )
    }

    fun startRingtone(context: Context) {
        fakeCallService.startRingtone(context)
    }

    fun stopRingtone(context: Context) {
        fakeCallService.stopRingtone()
    }
}
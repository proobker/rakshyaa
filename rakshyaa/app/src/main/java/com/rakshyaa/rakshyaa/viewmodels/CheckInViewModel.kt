package com.rakshyaa.rakshyaa.viewmodels

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.models.CheckIn
import com.rakshyaa.rakshyaa.data.repositories.CheckInRepository
import com.rakshyaa.rakshyaa.services.CheckInService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val checkInRepository: CheckInRepository
) : ViewModel() {

    data class UiState(
        val checkIns: List<CheckIn> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isScheduling: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadCheckIns()
    }

    fun loadCheckIns() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val checkIns = checkInRepository.getAll()
            _uiState.value = _uiState.value.copy(
                checkIns = checkIns,
                isLoading = false
            )
        }
    }

    fun scheduleCheckIn(minutesFromNow: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScheduling = true)
            val scheduledAt = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L)
            val saved = checkInRepository.schedule(scheduledAt)
            startCheckInService(saved.id, minutesFromNow)
            loadCheckIns()
            _uiState.value = _uiState.value.copy(isScheduling = false)
        }
    }

    fun checkInNow(checkInId: String) {
        viewModelScope.launch {
            checkInRepository.complete(checkInId, null, null)
            loadCheckIns()
        }
    }

    fun cancelCheckIn(checkInId: String) {
        val intent = Intent(context, CheckInService::class.java).apply {
            action = CheckInService.ACTION_CANCEL_CHECK_IN
        }
        runCatching { context.startService(intent) }
        viewModelScope.launch {
            loadCheckIns()
        }
    }

    private fun startCheckInService(checkInId: String, graceMin: Int) {
        val intent = Intent(context, CheckInService::class.java).apply {
            action = CheckInService.ACTION_SCHEDULE_CHECK_IN
            putExtra(CheckInService.EXTRA_CHECK_IN_ID, checkInId)
            putExtra(CheckInService.EXTRA_GRACE_PERIOD_MIN, graceMin)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

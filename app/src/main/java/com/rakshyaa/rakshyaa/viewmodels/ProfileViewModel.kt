package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    data class UiState(
        val userEmail: String? = null,
        val userName: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val notificationsEnabled: Boolean = true,
        val locationSharingEnabled: Boolean = true,
        val backupEnabled: Boolean = true,
        val appVersion: String = "1.1",
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val authState = authRepository.state.value
            _uiState.value = _uiState.value.copy(
                userEmail = authState.user?.email,
                userName = authState.user?.name
            )
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            // Trigger sync for all data types
            syncManager.remoteKeys()
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis()
            )
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
    }

    fun toggleLocationSharing(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(locationSharingEnabled = enabled)
    }

    fun toggleBackup(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(backupEnabled = enabled)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
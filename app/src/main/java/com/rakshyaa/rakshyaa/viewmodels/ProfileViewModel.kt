package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    private val store: EncryptedLocalStore
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class ProfileSettings(
        val notificationsEnabled: Boolean = true,
        val locationSharingEnabled: Boolean = true,
        val backupEnabled: Boolean = true
    )

    data class UiState(
        val userEmail: String? = null,
        val userName: String? = null,
        val userPhone: String? = null,
        val isSyncing: Boolean = false,
        val lastSyncTime: Long? = null,
        val notificationsEnabled: Boolean = true,
        val locationSharingEnabled: Boolean = true,
        val backupEnabled: Boolean = true,
        val appVersion: String = "1.1",
        val error: String? = null,
        val isEditing: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadSettings()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val authState = authRepository.state.value
            val savedName = store.loadPlain("profile_name")
            val savedPhone = store.loadPlain("profile_phone")
            _uiState.value = _uiState.value.copy(
                userEmail = authState.user?.email,
                userName = savedName ?: authState.user?.name,
                userPhone = savedPhone
            )
        }
    }

    private fun loadSettings() {
        val plain = store.loadPlain("profile_settings")
        val settings = plain?.let {
            runCatching { json.decodeFromString<ProfileSettings>(it) }.getOrNull()
        } ?: ProfileSettings()
        _uiState.value = _uiState.value.copy(
            notificationsEnabled = settings.notificationsEnabled,
            locationSharingEnabled = settings.locationSharingEnabled,
            backupEnabled = settings.backupEnabled
        )
    }

    private fun saveSettings() {
        val current = _uiState.value
        val settings = ProfileSettings(
            notificationsEnabled = current.notificationsEnabled,
            locationSharingEnabled = current.locationSharingEnabled,
            backupEnabled = current.backupEnabled
        )
        store.savePlain("profile_settings", json.encodeToString(settings))
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
        saveSettings()
    }

    fun toggleLocationSharing(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(locationSharingEnabled = enabled)
        saveSettings()
    }

    fun toggleBackup(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(backupEnabled = enabled)
        saveSettings()
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun updateProfile(name: String, phone: String) {
        store.savePlain("profile_name", name)
        store.savePlain("profile_phone", phone)
        _uiState.value = _uiState.value.copy(
            userName = name,
            userPhone = phone,
            isEditing = false
        )
    }
}

package com.rakshyaa.rakshyaa.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.BuildConfig
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.repositories.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountDeletion: com.rakshyaa.rakshyaa.data.auth.AccountDeletion,
    private val syncManager: com.rakshyaa.rakshyaa.data.sync.SyncManager,
    private val store: EncryptedLocalStore,
    private val profileRepository: ProfileRepository
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
        val userBio: String? = null,
        val pictureUrl: String? = null,
        val pictureFile: File? = null,
        val pictureLoading: Boolean = false,
        val isSyncing: Boolean = false,
        val isSaving: Boolean = false,
        val lastSyncTime: Long? = null,
        val notificationsEnabled: Boolean = true,
        val locationSharingEnabled: Boolean = true,
        val backupEnabled: Boolean = true,
        val appVersion: String = BuildConfig.VERSION_NAME,
        val error: String? = null,
        val isEditing: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadSettings()
        refreshFromRemote()
        viewModelScope.launch {
            syncManager.error.collect { message -> if (message != null) _uiState.value = _uiState.value.copy(error = message) }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            val authState = authRepository.state.value
            _uiState.value = _uiState.value.copy(
                userEmail = authState.user?.email,
                userName = profileRepository.localName() ?: authState.user?.name,
                userPhone = profileRepository.localPhone(),
                userBio = profileRepository.localBio(),
                pictureLoading = true
            )
            loadPicture()
        }
    }

    private suspend fun loadPicture() {
        val ref = profileRepository.displayPictureRef()
        val state = _uiState.value.copy(pictureLoading = true)
        if (ref != null && ref.startsWith("media:")) {
            val file = profileRepository.cachedPictureFile()
            _uiState.value = state.copy(pictureFile = file, pictureUrl = null)
        } else {
            _uiState.value = state.copy(pictureFile = null, pictureUrl = ref)
        }
        _uiState.value = _uiState.value.copy(pictureLoading = false)
    }

    /** Pulls the latest profile from the backend into the local store and state. */
    fun refreshFromRemote() {
        viewModelScope.launch {
            profileRepository.syncProfileFromRemote()
            _uiState.value = _uiState.value.copy(
                userName = profileRepository.localName() ?: _uiState.value.userName,
                userPhone = profileRepository.localPhone(),
                userBio = profileRepository.localBio()
            )
            loadPicture()
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
            backupEnabled = settings.backupEnabled && authRepository.state.value.user?.sub != AuthRepository.LOCAL_USER
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
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            try {
                syncManager.syncAll()
                _uiState.value = _uiState.value.copy(lastSyncTime = System.currentTimeMillis())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Backup failed. Please retry.")
            } finally {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
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
        if (enabled && authRepository.state.value.user?.sub == AuthRepository.LOCAL_USER) {
            _uiState.value = _uiState.value.copy(error = "Cloud backup requires Google sign-in and a configured backend.")
            return
        }
        _uiState.value = _uiState.value.copy(backupEnabled = enabled)
        saveSettings()
    }

    fun deleteAccount() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                accountDeletion.delete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Account deletion failed. Please retry.")
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            authRepository.signOut()
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun updateProfile(name: String, phone: String, bio: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val updated = profileRepository.saveProfile(name, phone, bio)
            _uiState.value = _uiState.value.copy(
                userName = updated?.name ?: name,
                userPhone = updated?.phone ?: phone,
                userBio = updated?.bio ?: bio,
                isSaving = false,
                isEditing = false,
                error = if (updated == null) "Could not save profile. Check your connection." else null
            )
        }
    }

    /** Uploads the picked photo as an encrypted blob and applies it to the profile. */
    fun changePicture(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, pictureLoading = true)
            val updated = profileRepository.uploadAndApplyPicture(uri)
            if (updated == null) _uiState.value = _uiState.value.copy(error = "Photo upload failed. Cloud photos require a configured backend and cloud backup.")
            loadPicture()
            _uiState.value = _uiState.value.copy(isSaving = false, pictureLoading = false)
        }
    }
}
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
            syncManager.remoteKeys()
            refreshFromRemote()
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
            profileRepository.uploadAndApplyPicture(uri)
            loadPicture()
            _uiState.value = _uiState.value.copy(isSaving = false, pictureLoading = false)
        }
    }
}
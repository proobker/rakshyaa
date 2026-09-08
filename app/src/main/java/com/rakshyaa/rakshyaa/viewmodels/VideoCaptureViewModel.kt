package com.rakshyaa.rakshyaa.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.models.VideoRecord
import com.rakshyaa.rakshyaa.data.repositories.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VideoCaptureViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    data class UiState(
        val videos: List<VideoRecord> = emptyList(),
        val isLoading: Boolean = false,
        val isRecording: Boolean = false,
        val isEncrypting: Boolean = false,
        val isUploading: Boolean = false,
        val error: String? = null,
        val decryptedVideo: File? = null,
        val recordingProgress: Int = 0
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val videos = videoRepository.getUserVideos()
            _uiState.value = _uiState.value.copy(videos = videos, isLoading = false)
        }
    }

    fun encryptAndBackupVideo(videoFile: File, videoType: String = "general") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEncrypting = true, isUploading = true)
            try {
                val userId = authRepository.state.value.user?.sub ?: "current_user"
                val record = videoRepository.uploadEncryptedVideo(
                    userId = userId,
                    videoFile = videoFile,
                    videoType = videoType
                )
                _uiState.value = _uiState.value.copy(
                    videos = _uiState.value.videos + record,
                    isEncrypting = false,
                    isUploading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to encrypt/upload video",
                    isEncrypting = false,
                    isUploading = false
                )
            }
        }
    }

    fun decryptVideo(videoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val file = videoRepository.downloadAndDecrypt(videoId)
            _uiState.value = _uiState.value.copy(isLoading = false, decryptedVideo = file)
        }
    }

    fun removeVideo(videoId: String) {
        viewModelScope.launch {
            videoRepository.remove(videoId)
            _uiState.value = _uiState.value.copy(
                videos = _uiState.value.videos.filterNot { it.id == videoId }
            )
        }
    }

    fun setRecording(isRecording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = isRecording)
    }

    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }
}
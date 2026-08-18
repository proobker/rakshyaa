package com.rakshyaa.rakshyaa.utils

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.media.ImageReader
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoCapture.OutputFileOptions
import androidx.camera.core.VideoCapture.OnVideoSavedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOptions
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for capturing video using CameraX
 * Supports front and rear camera switching
 */
@Singleton
class VideoCaptureUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val cameraProviderFuture by lazy { ProcessCameraProvider.getInstance(context) }
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var cameraSelector: CameraSelector? = null
    private var isCameraInitialized = false
    private var isRecording = false
    private var currentRecording: Recording? = null
    private var videoSaveJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // State flows for UI updates
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecordingState: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _previewSurface = MutableStateFlow<Surface?>(null)
    val previewSurface: StateFlow<Surface?> = _previewSurface.asStateFlow()

    /**
     * Initializes the camera with the specified lens facing
     */
    fun initializeCamera(lensFacing: Int = CameraSelector.LENS_FACING_BACK) {
        cameraProviderFuture.addListener({
            // Camera provider is now guaranteed to be available
            cameraProvider = cameraProviderFuture.get()

            // Set up camera selector
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Set up preview
            preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewSurfaceCallback) }

            // Set up image capture (for thumbnails)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Set up video capture
            setupVideoCapture()

            // Unbind any existing use cases before rebinding
            cameraProvider?.unbindAll()

            // Bind use cases to camera lifecycle
            try {
                val camera = cameraProvider?.bindToLifecycle(
                    /* lifecycleOwner= */ LifecycleOwner { },
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture
                )

                isCameraInitialized = true
                _isCameraReady.value = true
            } catch (exc: Exception) {
                // Handle binding errors
                exc.printStackTrace()
                isCameraInitialized = false
                _isCameraReady.value = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private val previewSurfaceCallback = Preview.SurfaceProvider { surface ->
        _previewSurface.value = surface
    }

    /**
     * Sets up video capture with appropriate quality and settings
     */
    private fun setupVideoCapture() {
        val recorder = Recorder.Builder()
            .setQuality(Quality.HIGHEST)
            .setBitrate(12_000_000) // 12 Mbps for good quality
            .build()

        videoCapture = VideoCapture.withOutput(recorder)
    }

    /**
     * Starts video recording
     *
     * @param outputFile File to save the video to
     * @return True if recording started successfully
     */
    fun startRecording(outputFile: File): Boolean {
        if (!isCameraInitialized || !isCameraReady.value || isRecording) {
            return false
        }

        try {
            val videoFileOptions = FileOptions.Builder(outputFile).build()
            currentRecording = videoCapture?.output
                ?.prepareRecording(context, videoFileOptions)
                ?.withAudioEnabled()
                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                    when {
                        event is VideoRecordEvent.Start -> {
                            isRecording = true
                            _isRecording.value = true
                        }
                        event is VideoRecordEvent.Finalize -> {
                            isRecording = false
                            _isRecording.value = false
                            if (!event.hasError()) {
                                // Recording succeeded
                                val msg = "Video saved: ${event.outputResults.outputUri}"
                            } else {
                                // Recording failed
                                val error = event.error
                                // Handle error
                            }
                        }
                    }
                }

            return currentRecording != null
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Stops video recording
     *
     * @return True if recording was stopped successfully
     */
    fun stopRecording(): Boolean {
        if (!isRecording || currentRecording == null) {
            return false
        }

        currentRecording.stop()
        isRecording = false
        _isRecording.value = false
        return true
    }

    /**
     * Switches between front and rear cameras
     */
    fun switchCamera() {
        if (!isCameraInitialized) return

        val newLensFacing = if (cameraSelector?.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        // Reinitialize camera with new lens facing
        initializeCamera(newLensFacing)
    }

    /**
     * Captures a still image (thumbnail)
     *
     * @param onImageSaved Callback for when image is saved
     */
    fun captureImage(onImageSaved: OnImageSavedCallback) {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(context),
            onImageSaved
        )
    }

    /**
     * Deinitializes the camera and releases resources
     */
    fun deinitializeCamera() {
        // Stop recording if in progress
        if (isRecording) {
            stopRecording()
        }

        // Unbind all use cases
        cameraProvider?.unbindAll()

        // Reset state
        isCameraInitialized = false
        isRecording = false
        _isCameraReady.value = false
        _isRecording.value = false
        _previewSurface.value = null

        // Clear references
        videoCapture = null
        preview = null
        imageCapture = null
        cameraSelector = null
        cameraProvider = null
    }

    /**
     * Lifecycle owner implementation for CameraX
     */
    private class LifecycleOwner : androidx.lifecycle.LifecycleOwner {
        private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)

        override fun getLifecycle(): androidx.lifecycle.Lifecycle = lifecycleRegistry

        // For simplicity, we're setting to STARTED state
        init {
            lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
        }
    }
}
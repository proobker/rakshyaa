package com.rakshyaa.rakshyaa.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.models.VideoRecord
import com.rakshyaa.rakshyaa.viewmodels.VideoCaptureViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun VideoCaptureScreen(
    viewModel: VideoCaptureViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showTypeDialog by remember { mutableStateOf(false) }
    var selectedVideoType by remember { mutableStateOf("general") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<VideoRecord?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0L) }

    val videoTypes = listOf("general", "evidence", "incident", "testimony")

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // Request camera + mic permissions on first entry.
    LaunchedEffect(key1 = true) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        val granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) {
            permissionLauncher.launch(permissions)
        }
    }

    // CameraX state
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    LaunchedEffect(Unit) {
        ProcessCameraProvider.getInstance(context).addListener(
            {
                cameraProvider = ProcessCameraProvider.getInstance(context).get()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    // Bind preview + recorder once we have both the provider and PreviewView.
    val preview = previewView
    val provider = cameraProvider
    LaunchedEffect(preview, provider) {
        if (preview != null && provider != null) {
            val previewUseCase = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(preview.surfaceProvider) }
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
                    )
                )
                .build()
            val vc = VideoCapture.withOutput(recorder)
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    vc
                )
                videoCapture = vc
            } catch (e: Exception) {
                // Recorder unsupported on this device: fall back to preview-only.
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase)
                } catch (_: Exception) {
                }
                viewModel.setError("Recording unavailable on this device: ${e.message}")
            }
        }
    }

    fun doStartRecording() {
        val vc = videoCapture
        if (vc == null) {
            viewModel.setError("Camera is not ready. Check camera permission and try again.")
            return
        }
        val fileName = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val file = File(context.cacheDir, fileName)
        val options = FileOutputOptions.Builder(file).build()
        try {
            val recording = vc.output
                .prepareRecording(context, options)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (event.hasError()) {
                            viewModel.setError("Recording failed: ${event.error}")
                            runCatching { context.contentResolver.delete(event.outputResults.outputUri, null, null) }
                        } else {
                            val outputFile = event.outputResults.outputUri.path?.let { File(it) }
                            if (outputFile != null) {
                                viewModel.encryptAndBackupVideo(outputFile, selectedVideoType)
                            }
                        }
                        viewModel.setRecording(false)
                    }
                }
            activeRecording = recording
            elapsedSeconds = 0L
            viewModel.setRecording(true)
        } catch (e: Exception) {
            viewModel.setError("Failed to start recording: ${e.message}")
        }
    }

    fun doStopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    // Recording duration timer
    LaunchedEffect(uiState.isRecording) {
        while (uiState.isRecording) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    val recordedDuration = remember(elapsedSeconds) {
        val m = (elapsedSeconds / 60).toString().padStart(2, '0')
        val s = (elapsedSeconds % 60).toString().padStart(2, '0')
        "$m:$s"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.video_capture_title)) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Camera Preview — always live, like a camera app
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                previewView = this
                            }
                        },
                        update = { pv -> previewView = pv },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Recording overlay: red dot + elapsed time
                    if (uiState.isRecording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(androidx.compose.ui.graphics.Color.Red)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = " $recordedDuration",
                                style = MaterialTheme.typography.labelLarge,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Video type selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                videoTypes.forEach { type ->
                    val isSelected = selectedVideoType == type
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedVideoType = type
                            if (!uiState.isRecording) showTypeDialog = true
                        },
                        label = { Text(type.uppercase()) }
                    )
                }
            }

            // Recording controls — camera-like: start / stop
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isRecording) {
                        Button(
                            onClick = { doStopRecording() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = stringResource(R.string.stop_recording),
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { showTypeDialog = true },
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = stringResource(R.string.start_recording),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Encryption/Upload Progress
            if (uiState.isEncrypting || uiState.isUploading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.isEncrypting) stringResource(R.string.encrypting_video) else stringResource(R.string.uploading_video),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Videos List
            if (uiState.videos.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recorded_videos),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.videos) { video ->
                                VideoItem(
                                    video = video,
                                    onDecrypt = { viewModel.decryptVideo(video.id) },
                                    onDelete = {
                                        videoToDelete = video
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (!uiState.isRecording) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_videos_recorded),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.record_first_video_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Video Type Selection Dialog
    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text(stringResource(R.string.select_video_type)) },
            text = {
                Column(
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    videoTypes.forEach { type ->
                        val isSelected = selectedVideoType == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.uppercase(), style = MaterialTheme.typography.bodyLarge)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    doStartRecording()
                    showTypeDialog = false
                }) {
                    Text(stringResource(R.string.start_recording))
                }
            },
            dismissButton = {
                Button(onClick = { showTypeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; videoToDelete = null },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_video_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        videoToDelete?.let { viewModel.removeVideo(it.id) }
                        showDeleteConfirm = false
                        videoToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false; videoToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// Video Item
@Composable
fun VideoItem(
    video: VideoRecord,
    onDecrypt: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val timestamp = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(video.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.primary.copy(alpha = 0.15f))
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .background(colors.primary.copy(alpha = 0.2f))
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = video.videoType.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onDecrypt) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.decrypt_download), tint = colors.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = colors.error)
                    }
                }
            }
        }
    }
}
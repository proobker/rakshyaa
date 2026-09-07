package com.rakshyaa.rakshyaa.ui.screens

import android.content.Context
import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.viewmodels.FakeCallViewModel

@Composable
fun FakeCallScreen(
    viewModel: FakeCallViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showConfigDialog by remember { mutableStateOf(false) }
    var isIncomingCall by remember { mutableStateOf(false) }
    var countdownTimer: CountDownTimer? = remember { null }
    var countdownSeconds by remember { mutableStateOf(uiState.triggerDelaySeconds) }

    androidx.compose.runtime.DisposableEffect(key1 = Unit) {
        onDispose {
            countdownTimer?.cancel()
            viewModel.stopRingtone(context)
        }
    }

    fun startCountdown() {
        countdownSeconds = uiState.triggerDelaySeconds
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(uiState.triggerDelaySeconds * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownSeconds = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                viewModel.startRingtone(context)
            }
        }.start()
    }

    // Handle incoming call UI
    androidx.compose.runtime.LaunchedEffect(key1 = uiState.isCallActive) {
        if (uiState.isCallActive && !isIncomingCall) {
            isIncomingCall = true
            startCountdown()
        } else if (!uiState.isCallActive) {
            isIncomingCall = false
            countdownTimer?.cancel()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.fake_call_title)) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isCallActive && !isIncomingCall) {
                ExtendedFloatingActionButton(
                    onClick = { showConfigDialog = true },
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    text = { Text(stringResource(R.string.trigger_fake_call)) },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { padding ->
        // Incoming Call Full Screen
        if (isIncomingCall && uiState.ongoingCall != null) {
            val call = uiState.ongoingCall!!
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Caller Image/Icon
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                if (call.isVideo) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            )
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (call.isVideo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    // Caller Info
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = call.callerName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = call.callerNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (call.connected) {
                            Text(
                                text = stringResource(R.string.call_connected),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.incoming_call),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Call Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        // Decline Button
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {
                                viewModel.endCall()
                                isIncomingCall = false
                            }) {
                                Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.decline), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            }
                        }

                        // Answer Button
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = {
                                if (!call.connected) {
                                    viewModel.answerCall()
                                } else {
                                    viewModel.endCall()
                                    isIncomingCall = false
                                }
                            }) {
                                Icon(
                                    imageVector = if (!call.connected) Icons.Default.Call else Icons.Default.CallEnd,
                                    contentDescription = if (!call.connected) stringResource(R.string.answer) else stringResource(R.string.end_call),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Configuration Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Caller Config Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.caller_configuration),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        androidx.compose.material3.OutlinedTextField(
                            value = uiState.callerName,
                            onValueChange = { viewModel.updateCallerName(it) },
                            label = { Text(stringResource(R.string.caller_name)) },
                            placeholder = { Text(stringResource(R.string.enter_caller_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        androidx.compose.material3.OutlinedTextField(
                            value = uiState.callerNumber,
                            onValueChange = { viewModel.updateCallerNumber(it) },
                            label = { Text(stringResource(R.string.caller_number)) },
                            placeholder = { Text("+1 XXX XXXX") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = uiState.isVideoCall,
                                onCheckedChange = { viewModel.toggleVideoCall(it) },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.video_call),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.video_call_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Trigger Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.trigger_settings),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.trigger_delay, countdownSeconds),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            androidx.compose.material3.Slider(
                                value = uiState.triggerDelaySeconds.toFloat(),
                                onValueChange = { viewModel.setTriggerDelay(it.toInt()) },
                                valueRange = 1f..300f,
                                steps = 299,
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    thumbColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("1s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("5min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Trigger Button
                if (!uiState.isCallActive) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(R.string.ready_to_trigger),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.trigger_desc, uiState.triggerDelaySeconds),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.triggerFakeCall() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(stringResource(R.string.trigger_now), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }

                // Active Call Status
                if (uiState.isCallActive && uiState.ongoingCall != null) {
                    val call = uiState.ongoingCall!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.call_in_progress),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${call.callerName} - ${call.callerNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (call.connected) {
                                Text(
                                    text = stringResource(R.string.call_connected_duration, formatDuration(System.currentTimeMillis() - call.startedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Button(
                                onClick = { viewModel.endCall() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text(stringResource(R.string.end_call))
                            }
                        }
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
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "%02d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
        else -> "%02d:%02d".format(minutes, seconds % 60)
    }
}
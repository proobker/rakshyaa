package com.rakshyaa.rakshyaa.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.models.CheckIn
import com.rakshyaa.rakshyaa.services.CheckInService
import com.rakshyaa.rakshyaa.viewmodels.CheckInViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember { context as ComponentActivity }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableStateOf(30) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var checkInToDelete by remember { mutableStateOf<CheckIn?>(null) }

    val minutesOptions = listOf(15, 30, 60, 120, 240, 480)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.check_in_title)) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showScheduleDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.schedule_check_in)) },
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Check-In Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.quick_check_in_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.quick_check_in_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            // Find the most recent pending check-in and complete it
                            val pending = uiState.checkIns.firstOrNull { it.status == "pending" }
                            pending?.let { viewModel.checkInNow(it.id) }
                        },
                        enabled = uiState.checkIns.any { it.status == "pending" } && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.check_in_now), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

            // Scheduled Check-ins List
            if (uiState.checkIns.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_check_ins),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_check_ins_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(uiState.checkIns) { checkIn ->
                        CheckInCard(
                            checkIn = checkIn,
                            onCheckIn = { viewModel.checkInNow(checkIn.id) },
                            onCancel = { 
                                checkInToDelete = checkIn
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Schedule Dialog
    if (showScheduleDialog) {
        ScheduleCheckInDialog(
            selectedMinutes = selectedMinutes,
            onMinutesChange = { selectedMinutes = it },
            minutesOptions = minutesOptions,
            onConfirm = {
                viewModel.scheduleCheckIn(selectedMinutes)
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; checkInToDelete = null },
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_check_in_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        checkInToDelete?.let { viewModel.cancelCheckIn(it.id) }
                        showDeleteConfirm = false
                        checkInToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.delete_check_in))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirm = false; checkInToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun CheckInCard(
    checkIn: CheckIn,
    onCheckIn: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val statusColor = when (checkIn.status) {
        "pending" -> colors.primary
        "completed" -> Color(0xFF4CAF50)
        "missed" -> colors.error
        "escalated" -> Color(0xFFFF9800)
        else -> colors.onSurfaceVariant
    }
    val statusIcon = when (checkIn.status) {
        "pending" -> Icons.Default.AccessTime
        "completed" -> Icons.Default.CheckCircle
        "missed" -> Icons.Default.Warning
        "escalated" -> Icons.Default.Warning
        else -> Icons.Default.AccessTime
    }

    val timestamp = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault()).format(java.util.Date(checkIn.scheduledAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checkIn.status == "pending") colors.primaryContainer.copy(alpha = 0.3f) else colors.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(statusColor.copy(alpha = 0.15f))
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        fontWeight = if (checkIn.status == "pending") androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = checkIn.status.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        if (checkIn.status == "completed") {
                            checkIn.checkedInAt?.let { checkedAt ->
                                val checkInTime = android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(checkedAt)
                                Text(
                                    text = "Checked in at $checkInTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (checkIn.status == "pending") {
                    Column {
                        Button(
                            onClick = onCheckIn,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            )
                        ) {
                            Text(stringResource(R.string.check_in_now))
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_check_in), tint = colors.error)
                        }
                    }
                }
            }

            if (checkIn.latitude != null && checkIn.longitude != null) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lat: ${checkIn.latitude}, Lon: ${checkIn.longitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCheckInDialog(
    selectedMinutes: Int,
    onMinutesChange: (Int) -> Unit,
    minutesOptions: List<Int>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_check_in)) },
        text = {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_check_in_interval),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    androidx.compose.material3.TextField(
                        value = "${selectedMinutes} ${stringResource(R.string.minutes)}",
                        onValueChange = { },
                        label = { Text(stringResource(R.string.interval)) },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        minutesOptions.forEach { min ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("${min} ${stringResource(R.string.minutes)}") },
                                onClick = {
                                    onMinutesChange(min)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.schedule_check_in))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
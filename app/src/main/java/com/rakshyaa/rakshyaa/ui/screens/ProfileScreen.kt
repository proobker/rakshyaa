package com.rakshyaa.rakshyaa.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.viewmodels.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showSignOutDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(uiState.userName ?: "") }
    var editPhone by remember { mutableStateOf(uiState.userPhone ?: "") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
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
            // Profile Header
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
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    if (uiState.isEditing) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text(stringResource(R.string.first_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            androidx.compose.material3.OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text(stringResource(R.string.phone_number)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleEditMode() },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) { Text(stringResource(R.string.cancel)) }
                                Button(
                                    onClick = { viewModel.updateProfile(editName, editPhone) },
                                    modifier = Modifier.weight(1f)
                                ) { Text(stringResource(R.string.save)) }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.userName ?: stringResource(R.string.user),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            uiState.userEmail?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            uiState.userPhone?.let { phone ->
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!uiState.isEditing) {
                            Button(
                                onClick = {
                                    editName = uiState.userName ?: ""
                                    editPhone = uiState.userPhone ?: ""
                                    viewModel.toggleEditMode()
                                },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("  Edit Profile")
                            }
                        }
                        ChipItem(
                            icon = Icons.Default.Verified,
                            label = stringResource(R.string.verified),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ChipItem(
                            icon = Icons.Default.Build,
                            label = "v${uiState.appVersion}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Settings Sections
            SettingsSection(
                title = stringResource(R.string.preferences),
                items = listOf(
                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.notifications),
                        subtitle = stringResource(R.string.notifications_desc),
                        trailing = {
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.toggleNotifications(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    ),
                    SettingItem(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.location_sharing),
                        subtitle = stringResource(R.string.location_sharing_desc),
                        trailing = {
                            Switch(
                                checked = uiState.locationSharingEnabled,
                                onCheckedChange = { viewModel.toggleLocationSharing(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    ),
                    SettingItem(
                        icon = Icons.Default.Backup,
                        title = stringResource(R.string.auto_backup),
                        subtitle = stringResource(R.string.auto_backup_desc),
                        trailing = {
                            Switch(
                                checked = uiState.backupEnabled,
                                onCheckedChange = { viewModel.toggleBackup(it) },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    )
                )
            )

            SettingsSection(
                title = stringResource(R.string.data_management),
                items = listOf(
                    SettingItem(
                        icon = Icons.Default.Sync,
                        title = stringResource(R.string.sync_now),
                        subtitle = stringResource(R.string.sync_now_desc),
                        trailing = {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                IconButton(onClick = { viewModel.syncNow() }) {
                                    Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.sync_now), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        onClick = { if (!uiState.isSyncing) viewModel.syncNow() }
                    ),
                    SettingItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.last_sync),
                        subtitle = uiState.lastSyncTime?.let {
                            java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault()).format(java.util.Date(it)) 
                        } ?: stringResource(R.string.never_synced),
                        trailing = null
                    )
                )
            )

            SettingsSection(
                title = stringResource(R.string.about),
                items = listOf(
                    SettingItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.app_version),
                        subtitle = "v${uiState.appVersion}",
                        trailing = null
                    ),
                    SettingItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.privacy_policy_desc),
                        trailing = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { browse(context, "https://rakshyaa.com/privacy") }
                    ),
                    SettingItem(
                        icon = Icons.Default.Verified,
                        title = stringResource(R.string.terms_of_service),
                        subtitle = stringResource(R.string.terms_of_service_desc),
                        trailing = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { browse(context, "https://rakshyaa.com/terms") }
                    ),
                    SettingItem(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.help_support),
                        subtitle = stringResource(R.string.help_support_desc),
                        trailing = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { browse(context, "https://rakshyaa.com/support") }
                    )
                )
            )

            // Sign Out Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        Text(stringResource(R.string.sign_out))
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

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.sign_out)) },
            text = { Text(stringResource(R.string.sign_out_confirmation)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                Button(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            items.forEachIndexed { index, item ->
                SettingRow(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == items.lastIndex
                )
                if (index != items.lastIndex) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                        content = {}
                    )
                }
            }
        }
    }
}

data class SettingItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val trailing: (@Composable () -> Unit)?,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SettingRow(
    item: SettingItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val shape = when {
        isFirst && isLast -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        isFirst -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        isLast -> androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(
                if (item.onClick != null) Modifier.clickable(onClick = item.onClick!!) else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            item.trailing?.invoke()
        }
    }
}

@Composable
fun ChipItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(color.copy(alpha = 0.15f))
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

private fun browse(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
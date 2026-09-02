package com.rakshyaa.rakshyaa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.ui.components.SOSButton
import com.rakshyaa.rakshyaa.viewmodels.SOSViewModel

@Composable
fun SOSScreen(
    viewModel: SOSViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showDeactivateDialog by remember { mutableStateOf(false) }
    
    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val onSurface = colors.onSurface
    val onSurfaceVariant = colors.onSurfaceVariant
    val surfaceContainer = colors.surfaceContainer
    val errorColor = colors.error
    val onError = colors.onError

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = if (uiState.isSosActive) errorColor else primaryColor,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (uiState.isSosActive) {
                stringResource(R.string.sos_active_title)
            } else if (uiState.isSosActivating) {
                stringResource(R.string.sos_activating_title)
            } else {
                stringResource(R.string.sos_inactive_title)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = if (uiState.isSosActive) errorColor else onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (uiState.isSosActive) {
                stringResource(R.string.sos_active_description)
            } else if (uiState.isSosActivating) {
                stringResource(R.string.sos_activating_description, uiState.sosActivationCountdown)
            } else {
                stringResource(R.string.sos_inactive_description)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        SOSButton(
            viewModel = viewModel,
            onActivateClick = { viewModel.activateSos() },
            onDeactivateClick = { showDeactivateDialog = true }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                title = stringResource(R.string.emergency_called),
                subtitle = if (uiState.isSosActive) "Emergency services alerted" else "Will call 112 on activation",
                icon = Icons.Default.Shield,
                isActive = uiState.isSosActive,
                color = errorColor,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = stringResource(R.string.location_sharing),
                subtitle = if (uiState.isSosActive) "Live location sharing active" else "Location will be shared",
                icon = Icons.Default.Info,
                isActive = uiState.isSosActive,
                color = primaryColor,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = stringResource(R.string.admin_notified),
                subtitle = if (uiState.isSosActive) "Admin portal notified" else "Incident sent to backend",
                icon = Icons.Default.Info,
                isActive = uiState.isSosActive,
                color = colors.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.sos_emergency_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDeactivateDialog) {
        DeactivateConfirmationDialog(
            onConfirm = {
                viewModel.deactivateSos()
                showDeactivateDialog = false
            },
            onDismiss = { showDeactivateDialog = false },
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            errorColor = errorColor,
            onError = onError,
            surfaceContainerHighest = colors.surfaceContainerHighest
        )
    }
}

@Composable
private fun DeactivateConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSurface: Color,
    onSurfaceVariant: Color,
    errorColor: Color,
    onError: Color,
    surfaceContainerHighest: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.deactivate_sos_confirmation_title),
                style = MaterialTheme.typography.titleLarge,
                color = onSurface
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = stringResource(R.string.deactivate_sos_confirmation_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = errorColor,
                    contentColor = onError
                )
            ) {
                Text(stringResource(R.string.yes_deactivate))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = surfaceContainerHighest,
                    contentColor = onSurface
                )
            ) {
                Text(stringResource(R.string.no_keep_active))
            }
        }
    )
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val onSurface = colors.onSurface
    val onSurfaceVariant = colors.onSurfaceVariant
    val surfaceContainer = colors.surfaceContainer
    
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) color.copy(alpha = 0.1f) else surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) color else onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) color else onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) color.copy(alpha = 0.8f) else onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

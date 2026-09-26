package com.rakshyaa.rakshyaa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshyaa.rakshyaa.viewmodels.SOSViewModel

@Composable
fun SOSButton(
    viewModel: SOSViewModel,
    onActivateClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    SOSButtonContent(uiState, onActivateClick, onDeactivateClick, viewModel::cancelSosActivation, modifier)
}

@Composable
fun SOSButtonContent(
    uiState: SOSViewModel.UiState,
    onActivateClick: () -> Unit,
    onDeactivateClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    
    if (uiState.isSosActivating) {
        SOSCountdownOverlay(uiState.sosActivationCountdown, onCancelClick, modifier)
        return
    }

    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val errorColor = colors.error
    val onPrimary = colors.onPrimary
    val onError = colors.onError
    
    val buttonColor by animateColorAsState(
        targetValue = if (uiState.isSosActive) errorColor else primaryColor,
        animationSpec = tween(durationMillis = 300)
    )
    
    val pulseScale by animateFloatAsState(
        targetValue = if (uiState.isSosActive) 1.05f else 1.0f,
        animationSpec = if (uiState.isSosActive)
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    delayMillis = 0
                ),
                repeatMode = RepeatMode.Reverse
            )
        else tween(durationMillis = 300)
    )
    
    val buttonDiameter = 140.dp
    val iconSize = 48.dp

    Box(
        modifier = modifier
            .size(buttonDiameter)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
    ) {
        // Outer pulse ring when active
        if (uiState.isSosActive) {
            Box(
                modifier = Modifier
                    .size(buttonDiameter + 20.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = 1f - (pulseScale - 1f) * 5
                    }
                    .clip(CircleShape)
                    .background(errorColor.copy(alpha = 0.15f))
            )
        }
        
        // Main button
        Button(
            onClick = if (uiState.isSosActive) onDeactivateClick else onActivateClick,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = if (uiState.isSosActive) onError else onPrimary
            ),
            shape = CircleShape,
            modifier = Modifier
                .size(buttonDiameter)

        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isSosActive) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = onPrimary,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = onError,
                        modifier = Modifier.size(iconSize)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (uiState.isSosActive) "DEACTIVATE SOS" else "ACTIVATE SOS",
                    color = if (uiState.isSosActive) onError else onPrimary,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun SOSCountdownOverlay(countdown: Int, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.rakshyaa.rakshyaa.R.string.sos_countdown_label),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = countdown.toString(),
            color = colors.error,
            fontSize = 64.sp,
            lineHeight = 72.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
        )
        androidx.compose.material3.OutlinedButton(onClick = onCancel) {
            Text(androidx.compose.ui.res.stringResource(com.rakshyaa.rakshyaa.R.string.sos_cancel_activation))
        }
    }
}

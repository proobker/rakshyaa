package com.rakshyaa.rakshyaa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateFloatAsState
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
    
    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val errorColor = colors.error
    val onPrimary = colors.onPrimary
    val onError = colors.onError
    
    val buttonColor by animateColorAsState(
        targetValue = if (uiState.isSosActive) errorColor else primaryColor,
        animationSpec = androidx.compose.animation.tween(durationMillis = 300)
    )
    
    val pulseScale by animateFloatAsState(
        targetValue = if (uiState.isSosActive) 1.05f else 1.0f,
        animationSpec = if (uiState.isSosActive) 
            androidx.compose.animation.infiniteRepeatable(
                animation = androidx.compose.animation.tween(
                    durationMillis = 1000,
                    delayMillis = 0,
                    easing = androidx.compose.animation.Easing.Default
                ),
                repeatMode = androidx.compose.animation.RepeatMode.Reverse
            )
        else androidx.compose.animation.tween(durationMillis = 300)
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (uiState.isSosActivating) 0.5f else 1.0f
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
        
        // Countdown overlay
        if (uiState.isSosActivating) {
            SOSCountdownOverlay(countdown = uiState.sosActivationCountdown)
        }
        
        // Main button
        Button(
            onClick = if (uiState.isSosActive) onDeactivateClick else onActivateClick,
            enabled = !uiState.isSosActivating,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = if (uiState.isSosActive) onError else onPrimary
            ),
            shape = CircleShape,
            modifier = Modifier
                .size(buttonDiameter)
                .alpha(contentAlpha)
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
fun SOSCountdownOverlay(countdown: Int) {
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ACTIVATING IN",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 16.sp,
                letterSpacing = 1.5.sp
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = countdown.toString(),
                color = colors.error,
                fontSize = 96.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap anywhere to cancel",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}
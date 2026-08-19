package com.rakshyaa.rakshyaa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.VectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.intPx
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.services.SOSActivationService
import com.rakshyaa.rakshyaa.theme.RakshyaaTheme
import com.rakshyaa.rakshyaa.viewmodels.SOSViewModel
import dagger.hilt.android.AndroidEntryPoint
import hiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Lispick

/**
 * Screen for SOS activation/deactivation with visual feedback and countdown timer
 */
@AndroidEntryPoint
class SOSActivity : ComponentActivity() {

    private val sosViewModel: SOSViewModel by hiltViewModel()
    private val authRepository: AuthRepository by inject()
    private val securePreferences: SecurePreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshyaaTheme {
                SOS Screen(
                    onNavigateToHome = { /* Handle navigation to home */ }
                )
            }
        }
    }
}

@Composable
fun SOScreen(
    onNavigateToHome: () -> Unit,
    sosViewModel: SOSViewModel = hiltViewModel(),
    authRepository: AuthRepository = hiltViewModel(),
    securePreferences: SecurePreferences = hiltViewModel()
) {
    val uiState by sosViewModel.uiState.collectAsState()
    val isSosActive by remember { mutableStateOf(false) }
    val countdownSeconds by remember { mutableStateOf(0) }
    val countdownAnimation by remember { Animatable(0f) }
    val pulseAnimation by remember { Animatable(1f) }

    // Start SOS activation countdown when transitioning to active state
    LaunchedEffect(uiState.isSosActivating) {
        if (uiState.isSosActivating) {
            startActivationCountdown()
        }
    }

    // Handle SOS activation/deactivation
    when {
        uiState.isSosActive -> {
            // SOS is active - show deactivation interface
            SosActiveContent(
                onDeactivateSos = {
                    sosViewModel.deactivateSos()
                },
                onNavigateToHome = onNavigateToHome
            )
        }
        uiState.isSosActivating -> {
            // SOS is activating - show countdown
            SosActivatingContent(
                countdownSeconds = uiState.sosActivationCountdown,
                onCancelActivation = {
                    sosViewModel.cancelSosActivation()
                    onNavigateToHome()
                }
            )
        }
        else -> {
            // SOS is inactive - show activation interface
            SosInactiveContent(
                onActivateSos = {
                    sosViewModel.activateSos()
                },
                onNavigateToHome = onNavigateToHome
            )
        }
    }
}

@Composable
private fun SosInactiveContent(
    onActivateSos: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // SOS Icon
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "SOS icon",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        // Title
        Text(
            text = stringResource(R.string.sos_inactive_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        // Description
        Text(
            text = stringResource(R.string.sos_inactive_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 32.dp)
        )

        // Activate SOS Button
        Button(
            onClick = onActivateSos,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    contentDescription = "Activating SOS"
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Activate SOS",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.activate_sos))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Learn more / How it works
        TextButton(
            onClick = { /* Show SOS explanation dialog */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.learn_more_about_sos))
        }
    }
}

@Composable
private fun SosActivatingContent(
    countdownSeconds: Int,
    onCancelActivation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // SOS Icon with pulse animation
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "SOS activating icon",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
                .graphicsLayer {
                    scaleX = pulseAnimation.value
                    scaleY = pulseAnimation.value
                }
        )

        // Title
        Text(
            text = stringResource(R.string.sos_activating_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        // Description
        Text(
            text = getString(R.string.sos_activating_description, countdownSeconds),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 24.dp)
        )

        // Countdown Timer
        Text(
            text = "%02d".format(countdownSeconds),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        )

        // Cancel Button
        Button(
            onClick = onCancelActivation,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
private fun SosActiveContent(
    onDeactivateSos: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // SOS Icon with pulse animation
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "SOS active icon",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
                .graphicsLayer {
                    scaleX = pulseAnimation.value
                    scaleY = pulseAnimation.value
                }
        )

        // Title
        Text(
            text = stringResource(R.string.sos_active_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        // Description
        Text(
            text = stringResource(R.string.sos_active_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 24.dp)
        )

        // Status Indicator Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(
                label = stringResource(R.string.emergency_called),
                isActive = true, // Would be dynamically determined in real implementation
                color = MaterialTheme.colorScheme.success
            )
            StatusIndicator(
                label = stringResource(R.string.location_sharing),
                isActive = true, // Would be dynamically determined in real implementation
                color = MaterialTheme.colorScheme.success
            )
            StatusIndicator(
                label = stringResource(R.string.admin_notified),
                isActive = true, // Would be dynamically determined in real implementation
                color = MaterialTheme.colorScheme.success
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Deactivate SOS Button
        Button(
            onClick = {
                // Show confirmation dialog before deactivating
                showDeactivateConfirmationDialog(
                    onConfirmDeactivate = onDeactivateSos,
                    onCancelDeactivate = { /* Do nothing, just dismiss */ }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Deactivate SOS close icon",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.deactivate_sos))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Info
        Text(
            text = stringResource(R.string.sos_emergency_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Start)
        )
    }
}

@Composable
private fun StatusIndicator(
    label: String,
    isActive: Boolean,
    color: Color
) {
    Column(
        modifier = Modifier
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Status indicator",
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Starts the SOS activation countdown animation
 */
private fun startActivationCountdown() {
    val totalSeconds = 5 // 5-second countdown before SOS activation
    countdownAnimation.animateTo(
        targetValue = totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = totalSeconds * 1000)
    ) { _, target ->
        // Update countdown display as animation progresses
        countdownSeconds = (totalSeconds - target).toInt()
        if (target == 0f) {
            // Countdown finished - activate SOS
            sosViewModel.activateSos()
        }
    }

    // Pulse animation for SOS icon
    pulseAnimation.animateTo(
        targetValue = 1.3f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
        )
    ).repeatMode = androidx.compose.animation.RepeatMode.Reverse
    pulseAnimation.animateTo(
        targetValue = 1.0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 200
        )
    ).repeatCount = androidx.compose.animation.AnimationRepeatMode.Infinite
}

/**
 * Shows a confirmation dialog before deactivating SOS
 */
@Composable
private fun showDeactivateConfirmationDialog(
    onConfirmDeactivate: () -> Unit,
    onCancelDeactivate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelDeactivate,
        title = { Text(stringResource(R.string.deactivate_sos_confirmation_title)) },
        text = { Text(stringResource(R.string.deactivate_sos_confirmation_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmDeactivate
            ) {
                Text(stringResource(R.string.yes_deactivate))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelDeactivate
            ) {
                Text(stringResource(R.string.no_keep_active))
            }
        }
    )
}
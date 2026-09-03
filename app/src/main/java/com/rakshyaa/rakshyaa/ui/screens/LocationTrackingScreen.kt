package com.rakshyaa.rakshyaa.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.models.LocationRecord
import com.rakshyaa.rakshyaa.viewmodels.LocationTrackingViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

@Composable
fun LocationTrackingScreen(
    viewModel: LocationTrackingViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember { context as ComponentActivity }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.toBooleanArray()
        if (granted.all { it == true }) {
            viewModel.startTracking()
        }
    }

    val hasPermissions = uiState.hasFineLocationPermission && uiState.hasBackgroundLocationPermission

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.tracking_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Permission Banner
        if (!hasPermissions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp).padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.tracking_permission_needed),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.tracking_permission_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.requestPermissions(activity, permissionLauncher) },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.grant_permission))
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val shouldShowRationale = remember {
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                                    activity,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                )
                            }
                            if (!shouldShowRationale && !hasPermissions) {
                                Button(
                                    onClick = { viewModel.openAppSettings() },
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(stringResource(R.string.open_settings))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Current Location Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.last_location),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        uiState.lastLocation?.let { loc ->
                            Text(
                                text = "Lat: ${loc.latitude}, Lon: ${loc.longitude}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.accuracy, loc.accuracy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        } ?: Text(
                            text = stringResource(R.string.no_location_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Map Preview
                if (uiState.lastLocation != null) {
                    MapPreview(
                        latitude = uiState.lastLocation!!.latitude,
                        longitude = uiState.lastLocation!!.longitude
                    )
                }
            }
        }

        // Primary Toggle Button (SOS-style)
        LocationTrackingButton(
            isTracking = uiState.isTracking,
            hasPermissions = hasPermissions,
            onStartClick = { viewModel.startTracking() },
            onStopClick = { viewModel.stopTracking() }
        )

        // History Section
        if (uiState.locationHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.location_history),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.locationHistory.take(20).reversed()) { record ->
                            LocationHistoryItem(record = record)
                        }
                    }
                }
            }
        }

        // Sync Now Button
        if (hasPermissions) {
            Button(
                onClick = { /* SyncManager auto-syncs on save */ },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.sync_now))
            }
        }
    }
}

@Composable
fun MapPreview(latitude: Double, longitude: Double) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            setBuiltInZoomControls(false)
            setHorizontalMapRepetitionEnabled(false)
            controller.setCenter(GeoPoint(latitude, longitude))
            controller.setZoom(15.0)
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { mapView },
        update = { view ->
            view.controller.animateTo(GeoPoint(latitude, longitude))
            view.overlays.clear()
            val marker = Marker(view)
            marker.position = GeoPoint(latitude, longitude)
            marker.title = "Current Location"
            view.overlays.add(marker)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
fun LocationHistoryItem(record: LocationRecord) {
    val colors = MaterialTheme.colorScheme
    val timestamp = android.text.format.DateFormat.getTimeFormat(LocalContext.current).format(record.timestamp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isSos) colors.errorContainer else colors.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (record.isSos) colors.error else colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "Lat: ${record.latitude}, Lon: ${record.longitude}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
                Text(
                    text = "$timestamp  •  Accuracy: ${record.accuracy.toInt()}m${if (record.isSos) "  •  SOS" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LocationTrackingButton(
    isTracking: Boolean,
    hasPermissions: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val buttonColor = if (isTracking) colors.error else colors.primary
    val iconColor = if (isTracking) colors.onError else colors.onPrimary
    val text = if (isTracking) stringResource(R.string.tracking_stop) else stringResource(R.string.tracking_start)

    androidx.compose.material3.Button(
        onClick = if (isTracking) onStopClick else onStartClick,
        enabled = hasPermissions && !isTracking || isTracking,
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier
            .size(140.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = iconColor,
            disabledContainerColor = colors.surfaceContainerHighest,
            disabledContentColor = colors.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = iconColor,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
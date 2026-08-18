package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.LocationRepository
import com.rakshyaa.rakshyaa.data.RideRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.utils.GeoUtils
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinlinenumberassigned

/**
 * Service for monitoring rides and detecting route deviations
 */
@AndroidEntryPoint
@hiltService
class RideMonitoringService @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val rideRepository: RideRepository,
    private val securePreferences: SecurePreferences
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ride_monitoring_channel"
        private const val NOTIFICATION_ID = 4
        private const val LOCATION_UPDATE_INTERVAL_MS = 10 * 1000L // 10 seconds
        private const val MIN_DISTANCE_FOR_UPDATE_M = 5.0f // 5 meters
        const val ACTION_START_RIDE_MONITORING = "ACTION_START_RIDE_MONITORING"
        const val ACTION_STOP_RIDE_MONITORING = "ACTION_STOP_RIDE_MONITORING"
        const val ACTION_UPDATE_ROUTE = "ACTION_UPDATE_ROUTE"
        const val EXTRA_DEVIATION_THRESHOLD_M = "extra_deviation_threshold_m"
        const val EXTRA_WAYPOINTS = "extra_waypoints"
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var coroutineScope: CoroutineScope? = null
    private var monitoringJob: Job? = null
    private var isMonitoring = false
    private var isRideActive = false

    // Ride data
    private var plannedWaypoints: List<Location> = emptyList()
    private var deviationThresholdM = 50.0 // Default 50 meters threshold
    private var rideStartTime: Long = 0
    private var currentRideId: String? = null

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupLocationListener()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RIDE_MONITORING -> {
                val threshold = intent.getDoubleExtra(EXTRA_DEVIATION_THRESHOLD_M, 50.0)
                val waypoints = intent.getParcelableArrayListExtra<Location>(EXTRA_WAYPOINTS) ?: emptyList()
                startRideMonitoring(threshold, waypoints)
            }
            ACTION_STOP_RIDE_MONITORING -> stopRideMonitoring()
            ACTION_UPDATE_ROUTE -> {
                val threshold = intent.getDoubleExtra(EXTRA_DEVIATION_THRESHOLD_M, 50.0)
                val waypoints = intent.getParcelableArrayListExtra<Location>(EXTRA_WAYPOINTS) ?: emptyList()
                updateRoute(threshold, waypoints)
            }
        }
        return START_STICKY
    }

    private fun startRideMonitoring(deviationThresholdM: Double, waypoints: List<Location>) {
        if (isMonitoring) return
        isMonitoring = true
        this.deviationThresholdM = deviationThresholdM
        this.plannedWaypoints = waypoints
        rideStartTime = System.currentTimeMillis()

        // Start foreground service
        startForeground(NOTIFICATION_ID, buildNotification("Ride Monitoring Active"))

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL_MS,
                MIN_DISTANCE_FOR_UPDATE_M,
                locationListener
            )
        }

        // Set up coroutine scope for ride monitoring logic
        coroutineScope = CoroutineScope(Dispatchers.Main)
        monitoringJob = coroutineScope?.launch {
            // Create a new ride record in the database
            val userId = securePreferences.getUserId()
            if (userId.isNotBlank()) {
                currentRideId = rideRepository.createRide(
                    userId = userId,
                    startTime = rideStartTime,
                    plannedWaypoints = plannedWaypoints,
                    deviationThresholdM = deviationThresholdM
                )
            }

            // Initial location check if we have last known location
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                checkLocationForDeviation(lastKnownLocation)
            }
        }

        isRideActive = true
    }

    private fun stopRideMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false

        // Remove location updates
        locationManager.removeUpdates(locationListener)

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        monitoringJob?.cancel()
        coroutineScope = null
        monitoringJob = null

        // Finalize ride record if we have one
        if (currentRideId.isNotBlank()) {
            rideRepository.updateRideEndTime(
                rideId = currentRideId!!,
                endTime = System.currentTimeMillis()
            )
            currentRideId = null
        }

        // Clear ride data
        plannedWaypoints = emptyList()
        isRideActive = false
    }

    private fun updateRoute(deviationThresholdM: Double, waypoints: List<Location>) {
        this.deviationThresholdM = deviationThresholdM
        this.plannedWaypoints = waypoints

        // Update the planned waypoints in the database if we have an active ride
        if (currentRideId.isNotBlank()) {
            rideRepository.updateRideRoute(
                rideId = currentRideId!!,
                plannedWaypoints = plannedWaypoints,
                deviationThresholdM = deviationThresholdM
            )
        }
    }

    private fun setupLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Save location to ride tracking
                saveLocationToRide(location)

                // Check for deviation from planned route
                checkLocationForDeviation(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
                // Handle status changes if needed
            }

            override fun onProviderEnabled(provider: String) {
                // Provider enabled
            }

            override fun onProviderDisabled(provider: String) {
                // Provider disabled - could show user notification to enable GPS
            }
        }
    }

    private fun saveLocationToRide(location: Location) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank() || currentRideId.isNullOrBlank()) {
            // User not logged in or no active ride
            return
        }

        // Save location to the ride's track
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rideRepository.addRideWaypoint(
                    rideId = currentRideId!!,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkLocationForDeviation(location: Location) {
        if (plannedWaypoints.isEmpty() || !isRideActive) return

        // Calculate distance to nearest point on planned route
        val distanceToRoute = GeoUtils.distanceToRoute(location, plannedWaypoints)

        // If deviation exceeds threshold, alert the user
        if (distanceToRoute > deviationThresholdM) {
            // Trigger deviation alert
            triggerDeviationAlert(distanceToRoute)
        }
    }

    private fun triggerDeviationAlert(actualDeviationM: Double) {
        // In a real implementation, this would show a notification or play a sound
        #TODO: Implement deviation alert (notification, sound, vibration)
        // For now, just log it
        android.util.Log.w("RideMonitoringService", "Deviation alert: ${actualDeviationM}m > ${deviationThresholdM}m threshold")
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.ride_monitoring_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_directions_car_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.ride_monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.ride_monitoring_channel_description)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRideMonitoring()
    }
}
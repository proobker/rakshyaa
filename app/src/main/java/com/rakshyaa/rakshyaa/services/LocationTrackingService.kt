package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.LocationRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.system.System
import java.util.concurrent.TimeUnit
import java.util.UUID

/**
 * Foreground service for tracking user location with periodic updates to Supabase
 */
@ExperimentalCoroutinesApi
class LocationTrackingService @Inject constructor(
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val securePreferences: SecurePreferences
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1
        private const val LOCATION_UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MIN_DISTANCE_FOR_UPDATE_M = 100.0f // 100 meters
        const val ACTION_START_LOCATION_UPDATES = "ACTION_START_LOCATION_UPDATES"
        const val ACTION_STOP_LOCATION_UPDATES = "ACTION_STOP_LOCATION_UPDATES"
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var coroutineScope: CoroutineScope? = null
    private var isTracking = false
    // Queue for batching location updates
    private val locationQueue = mutableListOf<LocationRecord>()
    private var flushJob: Job? = null
    private const val BATCH_FLUSH_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes
    private const val MAX_BATCH_SIZE = 10

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupLocationListener()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOCATION_UPDATES -> startLocationUpdates()
            ACTION_STOP_LOCATION_UPDATES -> stopLocationUpdates()
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (isTracking) return
        isTracking = true

        // Start foreground service
        startForeground(NOTIFICATION_ID, buildNotification())

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

        // Set up coroutine scope for periodic updates and batching
        coroutineScope = CoroutineScope(Dispatchers.Main)
        // Start the batch flush job
        startLocationBatchFlush()

        // Add initial location if available
        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            addLocationToQueue(lastKnownLocation)
        }
    }

    private fun stopLocationUpdates() {
        if (!isTracking) return
        isTracking = false

        // Remove location updates
        locationManager.removeUpdates(locationListener)

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        coroutineScope?.cancel()
        coroutineScope = null

        // Clean up flush job
        flushJob?.cancel()
        flushJob = null
    }

    private fun setupLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Add location to queue for batching
                addLocationToQueue(location)
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

    private fun saveLocationToSupabase(location: Location) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            // User not logged in, don't save location
            return
        }

        // Launch coroutine to save location without blocking
        CoroutineScope(Dispatchers.IO).launch {
            try {
                locationRepository.saveLocation(
                    userId = userId!!,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // Log error but don't crash the service
                e.printStackTrace()
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.location_tracking_active))
            .setContentText(getString(R.string.location_tracking_description))
            .setSmallIcon(R.drawable.ic_location_on_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.location_tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.location_tracking_channel_description)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun addLocationToQueue(location: Location) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            // User not logged in, don't queue location
            return
        }

        val locationRecord = LocationRecord(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId!!,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )

        synchronized(locationQueue) {
            locationQueue.add(locationRecord)
            if (locationQueue.size >= MAX_BATCH_SIZE) {
                flushLocationQueue()
            }
        }
    }

    private fun startLocationBatchFlush() {
        flushJob = coroutineScope?.launch {
            while (isTracking) {
                delay(BATCH_FLUSH_INTERVAL_MS)
                flushLocationQueue()
            }
        }
    }

    private fun flushLocationQueue() {
        val toFlush = synchronized(locationQueue) {
            val toFlush = locationQueue.toList()
            locationQueue.clear()
            toFlush
        }

        if (toFlush.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                locationRepository.saveLocationsBatch(toFlush)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
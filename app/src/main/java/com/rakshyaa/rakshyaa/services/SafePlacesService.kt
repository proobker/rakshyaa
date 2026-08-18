package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.SafePlacesRepository
import com.rakshyaa.rakshyaa.utils.GeoUtils
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import java.util.ArrayList
import java.util.List
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinlinenumberassigned

/**
 * Service for discovering nearby safe places and managing user-submitted safe places
 */
@AndroidEntryPoint
@hiltService
class SafePlacesService @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences,
    private val safePlacesRepository: SafePlacesRepository
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "safe_places_channel"
        private const val NOTIFICATION_ID = 5
        const val ACTION_START_SAFE_PLACES_MONITORING = "ACTION_START_SAFE_PLACES_MONITORING"
        const val ACTION_STOP_SAFE_PLACES_MONITORING = "ACTION_STOP_SAFE_PLACES_MONITORING"
        const val ACTION_ADD_USER_PLACE = "ACTION_ADD_USER_PLACE"
        const val EXTRA_RADIUS_M = "extra_radius_m"
        const val EXTRA_PLACE_TYPES = "extra_place_types"
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var coroutineScope: CoroutineScope? = null
    private var monitoringJob: Job? = null
    private var isMonitoring = false

    // Safe places data
    private var searchRadiusM = 5000.0 // Default 5km radius
    private var placeTypes: List<String> = listOf("hospital", "police", "fire_station")
    private var lastKnownLocation: Location? = null
    private var cachedSafePlaces: List<SafePlace> = emptyList()
    private val cacheDurationMs = 5 * 60 * 1000L // 5 minutes
    private var lastCacheUpdateTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        setupLocationListener()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SAFE_PLACES_MONITORING -> {
                val radius = intent.getDoubleExtra(EXTRA_RADIUS_M, 5000.0)
                val types = intent.getStringArrayListExtra<String>(EXTRA_PLACE_TYPES) ?: listOf("hospital", "police", "fire_station")
                startSafePlacesMonitoring(radius, types)
            }
            ACTION_STOP_SAFE_PLACES_MONITORING -> stopSafePlacesMonitoring()
            ACTION_ADD_USER_PLACE -> {
                val latitude = intent.getDoubleExtra("latitude", 0.0)
                val longitude = intent.getDoubleExtra("longitude", 0.0)
                val name = intent.getStringExtra("name") ?: ""
                val placeType = intent.getStringExtra("place_type") ?: "user_submitted"
                val description = intent.getStringExtra("description") ?: ""
                addUserSubmittedPlace(latitude, longitude, name, placeType, description)
            }
        }
        return START_STICKY
    }

    private fun startSafePlacesMonitoring(searchRadiusM: Double, placeTypes: List<String>) {
        if (isMonitoring) return
        isMonitoring = true
        this.searchRadiusM = searchRadiusM
        this.placeTypes = placeTypes

        // Start foreground service for continuous monitoring
        startForeground(NOTIFICATION_ID, buildNotification("Safe Places Monitoring Active"))

        // Request location updates for monitoring user position
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                30 * 1000L, // Update every 30 seconds for safe places check
                100.0f, // 100 meter minimum distance
                locationListener
            )
        }

        // Set up coroutine scope for safe places monitoring logic
        coroutineScope = CoroutineScope(Dispatchers.Main)
        monitoringJob = coroutineScope?.launch {
            // Initial load of safe places
            updateSafePlacesCache()
        }

        // Notify user that monitoring has started
        sendNotification("Safe Places Monitoring Started", "Monitoring for nearby hospitals, police stations, and fire stations")
    }

    private fun stopSafePlacesMonitoring() {
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
    }

    private fun setupLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastKnownLocation = location
                // Check if we need to update the safe places cache
                if (shouldUpdateCache()) {
                    updateSafePlacesCache()
                }
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

    private fun shouldUpdateCache(): Boolean {
        val timeSinceLastUpdate = System.currentTimeMillis() - lastCacheUpdateTime
        return timeSinceLastUpdate > cacheDurationMs || lastKnownLocation == null
    }

    private fun updateSafePlacesCache() {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank() || lastKnownLocation == null) {
            return
        }

        coroutineScope?.launch {
            try {
                // Get safe places from the repository (which uses PostGIS queries)
                val places = safePlacesRepository.getNearbySafePlaces(
                    latitude = lastKnownLocation!!.latitude,
                    longitude = lastKnownLocation!!.longitude,
                    radiusM = searchRadiusM,
                    placeTypes = placeTypes
                )

                cachedSafePlaces = places
                lastCacheUpdateTime = System.currentTimeMillis()

                #TODO: Notify any UI components or interested parties about the updated safe places
                #For now, just log the update
                android.util.Log.i("SafePlacesService", "Updated safe places cache: ${cachedSafePlaces.size} places found")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addUserSubmittedPlace(
        latitude: Double,
        longitude: Double,
        name: String,
        placeType: String,
        description: String
    ) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                safePlacesRepository.addUserSubmittedPlace(
                    userId = userId,
                    latitude = latitude,
                    longitude = longitude,
                    name = name,
                    placeType = placeType,
                    description = description
                )

                #TODO: Notify UI that a new place has been added
                android.util.Log.i("SafePlacesService", "Added user-submitted place: $name")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Gets the current cached safe places
     */
    fun getCurrentSafePlaces(): List<SafePlace> {
        return cachedSafePlaces
    }

    /**
     * Forces an immediate update of the safe places cache
     */
    fun forceUpdateSafePlaces() {
        updateSafePlacesCache()
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.safe_places_service_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_place_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.safe_places_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.safe_places_channel_description)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_place_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(999, notification) // Use a different ID for non-ongoing notifications
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSafePlacesMonitoring()
    }
}
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
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.repositories.RideRepository
import com.rakshyaa.rakshyaa.utils.GeoUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that tracks an active ride, appending GPS waypoints to the
 * encrypted ride repository and alerting when the user deviates from the planned
 * route beyond a threshold.
 */
@AndroidEntryPoint
class RideMonitoringService : Service() {

    @Inject lateinit var rideRepository: RideRepository

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ride_monitoring_channel"
        private const val NOTIFICATION_ID = 4

        const val ACTION_START_RIDE_MONITORING = "ACTION_START_RIDE_MONITORING"
        const val ACTION_STOP_RIDE_MONITORING = "ACTION_STOP_RIDE_MONITORING"
        const val ACTION_UPDATE_ROUTE = "ACTION_UPDATE_ROUTE"
        const val EXTRA_DEVIATION_THRESHOLD_M = "extra_deviation_threshold_m"
        const val EXTRA_WAYPOINTS = "extra_waypoints"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private var isMonitoring = false
    private var sessionId: String? = null
    private var deviationThresholdM = 50.0
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location -> onLocationChanged(location) }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RIDE_MONITORING -> {
                val threshold = intent.getDoubleExtra(EXTRA_DEVIATION_THRESHOLD_M, 50.0)
                startRideMonitoring(threshold)
            }
            ACTION_STOP_RIDE_MONITORING -> stopRideMonitoring()
            ACTION_UPDATE_ROUTE -> {
                deviationThresholdM = intent.getDoubleExtra(EXTRA_DEVIATION_THRESHOLD_M, deviationThresholdM)
            }
        }
        return START_STICKY
    }

    private fun startRideMonitoring(threshold: Double) {
        if (isMonitoring) return
        isMonitoring = true
        deviationThresholdM = threshold
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.ride_monitoring_active)))

        scope.launch {
            val session = rideRepository.start()
            sessionId = session.id
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10_000L,
                5.0f,
                locationListener
            )
        }
    }

    private fun stopRideMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        locationManager.removeUpdates(locationListener)
        val id = sessionId
        sessionId = null
        if (id != null) {
            scope.launch { rideRepository.end(id) }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onLocationChanged(location: Location) {
        val id = sessionId ?: return
        scope.launch {
            runCatching {
                rideRepository.appendPoint(id, location.latitude, location.longitude)
            }
        }
    }

    /** True if [location] has deviated more than the configured threshold from the nearest waypoint in [route]. */
    fun isOffRoute(location: Location, route: List<Location>): Boolean {
        if (route.isEmpty()) return false
        return GeoUtils.distanceToRoute(location, route) > deviationThresholdM
    }

    private fun buildNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.ride_monitoring_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_error_outline_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.ride_monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.ride_monitoring_channel_description)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

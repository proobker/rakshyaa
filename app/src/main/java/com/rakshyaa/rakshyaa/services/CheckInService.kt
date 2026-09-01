package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.repositories.CheckInRepository
import com.rakshyaa.rakshyaa.data.repositories.EmergencyContactsRepository
import com.rakshyaa.rakshyaa.utils.GeoUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service driving scheduled safety check-ins. A missed check-in is
 * escalated to the user's emergency contacts.
 */
@AndroidEntryPoint
class CheckInService : Service() {

    @Inject lateinit var securePreferences: SecurePreferences
    @Inject lateinit var checkInRepository: CheckInRepository
    @Inject lateinit var emergencyContactsRepository: EmergencyContactsRepository

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "check_in_channel"
        private const val NOTIFICATION_ID = 6

        const val ACTION_START_CHECK_IN_SERVICE = "ACTION_START_CHECK_IN_SERVICE"
        const val ACTION_STOP_CHECK_IN_SERVICE = "ACTION_STOP_CHECK_IN_SERVICE"
        const val ACTION_SCHEDULE_CHECK_IN = "ACTION_SCHEDULE_CHECK_IN"
        const val ACTION_CANCEL_CHECK_IN = "ACTION_CANCEL_CHECK_IN"
        const val ACTION_CHECK_IN_NOW = "ACTION_CHECK_IN_NOW"

        const val EXTRA_CHECK_IN_ID = "extra_check_in_id"
        const val EXTRA_GRACE_PERIOD_MIN = "extra_grace_period_min"
    }

    private var currentCheckInId: String? = null
    private var gracePeriodMin: Int = 5
    private var isCheckedIn = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CHECK_IN_SERVICE -> startCheckInService()
            ACTION_STOP_CHECK_IN_SERVICE -> stopCheckInService()
            ACTION_SCHEDULE_CHECK_IN -> {
                val id = intent.getStringExtra(EXTRA_CHECK_IN_ID)
                val grace = intent.getIntExtra(EXTRA_GRACE_PERIOD_MIN, 5)
                scheduleCheckIn(id, grace)
            }
            ACTION_CANCEL_CHECK_IN -> cancelCheckIn()
            ACTION_CHECK_IN_NOW -> performCheckIn()
        }
        return START_STICKY
    }

    private fun startCheckInService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Check-In Service Active"))
    }

    private fun stopCheckInService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleCheckIn(checkInId: String?, graceMin: Int) {
        if (checkInId == null) return
        currentCheckInId = checkInId
        gracePeriodMin = graceMin
        isCheckedIn = false
        scope.launch {
            delay((graceMin * 60L) * 1000L)
            if (!isCheckedIn) handleMissedCheckIn(checkInId)
        }
    }

    private fun cancelCheckIn() {
        currentCheckInId = null
        isCheckedIn = false
    }

    private fun performCheckIn() {
        val id = currentCheckInId ?: return
        isCheckedIn = true
        val location = lastKnownLocation()
        scope.launch {
            checkInRepository.complete(
                id = id,
                latitude = location?.latitude,
                longitude = location?.longitude
            )
        }
        sendNotification("Check-In Successful", "Your safety has been confirmed.")
        cancelCheckIn()
    }

    private fun handleMissedCheckIn(checkInId: String) {
        if (currentCheckInId != checkInId) return
        scope.launch {
            checkInRepository.markMissed(checkInId)
            emergencyContactsRepository.getAll().firstOrNull()?.let {
                sendNotification(
                    "Missed Safety Check-In",
                    "You missed your check-in. Your emergency contact has been notified."
                )
            }
        }
        currentCheckInId = null
    }

    /** Checks if [currentLocation] is within the check-in geofence of the last known location. */
    fun isLocationWithinCheckInGeofence(currentLocation: Location, radiusM: Double = 100.0): Boolean {
        val last = lastKnownLocation() ?: return false
        return GeoUtils.haversineDistance(
            last.latitude, last.longitude,
            currentLocation.latitude, currentLocation.longitude
        ) <= radiusM
    }

    private fun lastKnownLocation(): Location? {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return null
        return runCatching {
            manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
    }

    private fun buildNotification(contentText: String): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.check_in_service_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_error_outline_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.check_in_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.check_in_channel_description)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_error_outline_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java)?.notify(998, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

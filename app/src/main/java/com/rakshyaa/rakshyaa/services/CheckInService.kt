package com.rakshyaa.rakshyaa.services

import android.app.AlarmManager
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
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.CheckInRepository
import com.rakshyaa.rakshyaa.data.EmergencyContactsRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.utils.GeoUtils
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinlinenumberassigned

/**
 * Service for managing scheduled safety check-ins with grace periods and geofence validation
 */
@AndroidEntryPoint
@hiltService
class CheckInService @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences,
    private val checkInRepository: CheckInRepository,
    private val emergencyContactsRepository: EmergencyContactsRepository
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "check_in_channel"
        private const val NOTIFICATION_ID = 6
        const val ACTION_START_CHECK_IN_SERVICE = "ACTION_START_CHECK_IN_SERVICE"
        const val ACTION_STOP_CHECK_IN_SERVICE = "ACTION_STOP_CHECK_IN_SERVICE"
        const val ACTION_SCHEDULE_CHECK_IN = "ACTION_SCHEDULE_CHECK_IN"
        const val ACTION_CANCEL_CHECK_IN = "ACTION_CANCEL_CHECK_IN"
        const val ACTION_CHECK_IN_NOW = "ACTION_CHECK_IN_NOW"
        const val ACTION_MISSED_CHECK_IN = "ACTION_MISSED_CHECK_IN"
        const val EXTRA_CHECK_IN_ID = "extra_check_in_id"
        const val EXTRA_GRACE_PERIOD_MIN = "extra_grace_period_min"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_RADIUS_M = "extra_radius_m"
        private const val CHECK_IN_ACTION = "com.rakshyaa.rakshyaa.action.CHECK_IN"
        private const val SNOOZE_ACTION = "com.rakshyaa.rakshyaa.action.SNOOZE"
    }

    private lateinit var alarmManager: AlarmManager
    private var pendingCheckInIntent: PendingIntent? = null
    private var coroutineScope: CoroutineScope? = null
    private var checkInJob: Job? = null
    private var isServiceActive = false

    // Current check-in data
    private var currentCheckInId: String? = null
    private var scheduledCheckInTime: Long = 0
    private var gracePeriodMin: Int = 5 // Default 5 minutes grace period
    private var checkInLocation: Location? = null
    private var checkInRadiusM: Double = 100.0 // Default 100m radius for geofence
    private var isCheckedIn = false

    override fun onCreate() {
        super.onCreate()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CHECK_IN_SERVICE -> startCheckInService()
            ACTION_STOP_CHECK_IN_SERVICE -> stopCheckInService()
            ACTION_SCHEDULE_CHECK_IN -> {
                val checkInId = intent.getStringExtra(EXTRA_CHECK_IN_ID)
                val gracePeriod = intent.getIntExtra(EXTRA_GRACE_PERIOD_MIN, 5)
                val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                val radiusM = intent.getDoubleExtra(EXTRA_RADIUS_M, 100.0)
                scheduleCheckIn(checkInId, gracePeriod, latitude, longitude, radiusM)
            }
            ACTION_CANCEL_CHECK_IN -> {
                val checkInId = intent.getStringExtra(EXTRA_CHECK_IN_ID)
                cancelCheckIn(checkInId)
            }
            ACTION_CHECK_IN_NOW -> performCheckIn()
            ACTION_MISSED_CHECK_IN -> {
                val checkInId = intent.getStringExtra(EXTRA_CHECK_IN_ID)
                handleMissedCheckIn(checkInId)
            }
            CHECK_IN_ACTION -> performCheckIn() // Direct check-in action from notification
            SNOOZE_ACTION -> snoozeCheckIn() // Snooze action from notification
        }
        return START_STICKY
    }

    private fun startCheckInService() {
        if (isServiceActive) return
        isServiceActive = true

        // Start foreground service
        startForeground(NOTIFICATION_ID, buildNotification("Check-In Service Active"))

        // Set up coroutine scope for check-in service logic
        coroutineScope = CoroutineScope(Dispatchers.Main)
        checkInJob = coroutineScope?.launch {
            #TODO: Load any existing scheduled check-ins from the database
            #For now, we'll rely on the alarm system to trigger check-ins
        }
    }

    private fun stopCheckInService() {
        if (!isServiceActive) return
        isServiceActive = false

        // Cancel any pending alarms
        cancelAllCheckInAlarms()

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        checkInJob?.cancel()
        coroutineScope = null
        checkInJob = null

        // Clear current check-in data
        currentCheckInId = null
        isCheckedIn = false
    }

    private fun scheduleCheckIn(
        checkInId: String,
        gracePeriodMin: Int,
        latitude: Double,
        longitude: Double,
        radiusM: Double
    ) {
        currentCheckInId = checkInId
        this.gracePeriodMin = gracePeriodMin
        checkInLocation = Location("").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        checkInRadiusM = radiusM

        // Calculate the scheduled time (this would come from the check-in data)
        #TODO: In a real implementation, we would get the scheduled time from the database
        #For now, we'll schedule it for 5 minutes from now as an example
        scheduledCheckInTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)

        // Set up the alarm
        val checkInIntent = Intent(this, CheckInService::class.java).apply {
            action = ACTION_CHECK_IN_NOW
            putExtra(EXTRA_CHECK_IN_ID, checkInId)
        }
        pendingCheckInIntent = PendingIntent.getService(
            this,
            checkInId.hashCode(),
            checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            scheduledCheckInTime,
            pendingCheckInIntent!!
        )

        #TODO: Save the scheduled check-in to the database
        #checkInRepository.scheduleCheckIn(checkInId, scheduledCheckInTime, gracePeriodMin, ...)

        // Notify user about the scheduled check-in
        sendNotification(
            "Check-In Scheduled",
            "You have a check-in scheduled in 5 minutes. You'll need to confirm your safety."
        )
    }

    private fun cancelCheckIn(checkInId: String) {
        if (currentCheckInId == checkInId) {
            cancelCheckInAlarm()
            currentCheckInId = null
            isCheckedIn = false
            checkInLocation = null
            sendNotification("Check-In Cancelled", "Your scheduled check-in has been cancelled.")
        }
    }

    private fun cancelCheckInAlarm() {
        pendingCheckInIntent?.let { intent ->
            alarmManager.cancel(intent)
        }
    }

    private fun cancelAllCheckInAlarms() {
        pendingCheckInIntent?.let { intent ->
            alarmManager.cancel(intent)
        }
    }

    private fun performCheckIn() {
        if (currentCheckInId.isNullOrBlank()) {
            return
        }

        // Show notification asking user to check in
        showCheckInNotification()

        #TODO: In a real implementation, we would wait for user response or location confirmation
        #For now, we'll auto-check-in after the grace period as a fallback
        coroutineScope?.launch {
            delay(TimeUnit.MINUTES.toMillis(gracePeriodMin))
            if (!isCheckedIn) {
                // Grace period expired without check-in
                handleMissedCheckIn(currentCheckInId!!)
            }
        }
    }

    private fun showCheckInNotification() {
        val checkInIntent = Intent(this, CheckInService::class.java).apply {
            action = CHECK_IN_ACTION
            putExtra(EXTRA_CHECK_IN_ID, currentCheckInId)
        }
        val checkInPendingIntent = PendingIntent.getService(
            this,
            currentCheckInId!!.hashCode() + 1,
            checkInIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, CheckInService::class.java).apply {
            action = SNOOZE_ACTION
            putExtra(EXTRA_CHECK_IN_ID, currentCheckInId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            currentCheckInId!!.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle_24)
            .setContentTitle("Safety Check-In Required")
            .setContentText("Please confirm your safety by checking in or using your current location.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(R.drawable.ic_close_24, "Skip", snoozePendingIntent)
            .addAction(R.drawable.ic_check_24, "Check In", checkInPendingIntent)
            .setFullScreenIntent(checkInPendingIntent, true)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        isCheckedIn = false
    }

    private fun snoozeCheckIn() {
        #TODO: Implement snooze functionality (delay check-in by a few minutes)
        #For now, just treat as a missed check-in after a short delay
        coroutineScope?.launch {
            delay(TimeUnit.MINUTES.toMillis(2)) // Snooze for 2 minutes
            if (!isCheckedIn) {
                handleMissedCheckIn(currentCheckInId!!)
            }
        }
    }

    private fun handleMissedCheckIn(checkInId: String) {
        if (currentCheckInId != checkInId) {
            return // This is not the current check-in
        }

        isCheckedIn = false

        #TODO: Update the check-in record as missed in the database
        #checkInRepository.markCheckInAsMissed(checkInId)

        // Escalate to emergency contacts
        escalateToEmergencyContacts(checkInId)

        // Show notification about missed check-in
        sendNotification(
            "Missed Safety Check-In",
            "You missed your safety check-in. Emergency contacts have been notified."
        )

        // Reset for next check-in
        currentCheckInId = null
    }

    private fun escalateToEmergencyContacts(checkInId: String) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            return
        }

        coroutineScope?.launch {
            try {
                #TODO: Get the check-in details to include in the alert
                #For now, we'll just notify emergency contacts that a check-in was missed

                emergencyContactsRepository.escalateMissedCheckIn(
                    userId = userId,
                    checkInId = checkInId,
                    timestamp = System.currentTimeMillis()
                )

                android.util.Log.i("CheckInService", "Escalated missed check-in $checkInId to emergency contacts")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Checks if the user's current location is within the check-in geofence
     *
     * @param currentLocation User's current location
     * @return True if within the geofence, false otherwise
     */
    fun isLocationWithinCheckInGeofence(currentLocation: Location): Boolean {
        if (checkInLocation == null || currentCheckInId.isNullOrBlank()) {
            return false
        }

        val distance = GeoUtils.distanceBetween(checkInLocation!!, currentLocation)
        return distance <= checkInRadiusM
    }

    /**
     * Performs a check-in using the current location
     *
     * @param currentLocation User's current location
     * @return True if check-in was successful, false otherwise
     */
    fun performCheckInWithLocation(currentLocation: Location): Boolean {
        if (!isLocationWithinCheckInGeofence(currentLocation)) {
            return false
        }

        isCheckedIn = true

        #TODO: Update the check-in record as completed in the database
        #checkInRepository.completeCheckIn(
        #    checkInId = currentCheckInId!!,
        #    latitude = currentLocation.latitude,
        #    longitude = currentLocation.longitude,
        #    timestamp = System.currentTimeMillis()
        #)

        // Cancel the pending alarm since we've checked in
        cancelCheckInAlarm()

        #TODO: Clear the current check-in data or schedule the next one if it's recurring
        #For now, we'll just clear it
        currentCheckInId = null
        checkInLocation = null

        // Show success notification
        sendNotification("Check-In Successful", "Your safety has been confirmed.")

        return true
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.check_in_service_active))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_check_circle_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.check_in_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.check_in_channel_description)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_check_circle_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(998, notification) // Use a different ID for non-ongoing notifications
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCheckInService()
    }
}
package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.IncidentRepository
import com.rakshyaa.rakshyaa.data.LocationRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.ui.screens.HomeScreenActivity
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Service for handling SOS emergencies including activation, emergency calling, and notifications
 */
@AndroidEntryPoint
@hiltService
class SOSActivationService @Inject constructor(
    private val authRepository: AuthRepository,
    private val incidentRepository: IncidentRepository,
    private val locationRepository: LocationRepository,
    private val securePreferences: SecurePreferences,
    private val context: Context
) : Service() {

    companion object {
        private const val SOS_NOTIFICATION_CHANNEL_ID = "sos_notification_channel"
        private const val SOS_NOTIFICATION_ID = 2
        const val ACTION_ACTIVATE_SOS = "ACTION_ACTIVATE_SOS"
        const val ACTION_DEACTIVATE_SOS = "ACTION_DEACTIVATE_SOS"
        const val EXTRA_IS_FALSE_ALARM = "extra_is_false_alarm"
    }

    private var isSosActive = false
    private var sosStartTime: Long = 0
    private var coroutineScope: CoroutineScope? = null
    private var sosJob: Job? = null
    private var sosLocationJob: Job? = null
    private var currentIncidentId: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ACTIVATE_SOS -> activateSos(intent.getBooleanExtra(EXTRA_IS_FALSE_ALARM, false))
            ACTION_DEACTIVATE_SOS -> deactivateSos()
        }
        return START_STICKY
    }

    private fun activateSos(isFalseAlarm: Boolean) {
        if (isSosActive) return
        isSosActive = true
        sosStartTime = System.currentTimeMillis()

        // Start foreground service for SOS
        startForeground(SOS_NOTIFICATION_ID, buildSosNotification())

        // Save incident to Supabase and get the ID
        CoroutineScope(Dispatchers.IO).launch {
            currentIncidentId = saveSosIncident(isFalseAlarm)
        }

        // Make emergency call if not a false alarm
        if (!isFalseAlarm) {
            makeEmergencyCall()
        }

        // Start periodic location updates during SOS
        startSosLocationUpdates()

        // Notify admin portal (would typically be done via Supabase real-time or edge function)
        notifyAdminPortal()
    }

    private fun deactivateSos() {
        if (!isSosActive) return
        isSosActive = false

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        coroutineScope?.cancel()
        sosJob?.cancel()
        sosLocationJob?.cancel()
        coroutineScope = null
        sosJob = None
        sosLocationJob = None
    }

    private suspend fun saveSosIncident(isFalseAlarm: Boolean): String? {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            // User not logged in
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                val incidentData = mapOf(
                    "user_id" to userId,
                    "is_false_alarm" to isFalseAlarm,
                    "status" to "active",
                    "activated_at" to sosStartTime,
                    "created_at" to System.currentTimeMillis()
                )

                val response = incidentRepository.createIncident(incidentData)
                // Extract the ID from the response
                val data = response.data as List<<Map<String, Any>>>
                val incidentId = data[0]["id"] as String
                incidentId
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveSosLocation(location: Location) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            // User not logged in
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Save SOS location - could be to a special table or with a flag
                // For now, we'll use the same location_logs table but could add an sos_flag column
                val sosLocationData = mapOf(
                    "user_id" to userId,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy,
                    "timestamp" to System.currentTimeMillis(),
                    "is_sos" to true // Indicate this is an SOS location update
                )

                locationRepository.saveSosLocation(sosLocationData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notifyAdminPortal() {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            // User not logged in
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update the incident to notify admin portal
                // In a real implementation, this could:
                // 1. Update the incident record with a "admin_notified" flag
                // 2. Create a notification record in a separate table
                // 3. Trigger a Supabase edge function via real-time subscription
                // 4. Send a push notification to admin devices

                val incidentId = currentIncidentId ?: "unknown"
                val adminNotificationData = mapOf(
                    "user_id" to userId,
                    "incident_id" to incidentId,
                    "notified_at" to System.currentTimeMillis(),
                    "status" to "pending"
                )

                // For now, we'll just log that notification would be sent
                // In a real app, you'd call a repository method to save this notification
                Timber.tag("SOSActivationService").d("Admin portal notification would be sent for user $userId with incident $incidentId")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun makeEmergencyCall() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted - could request it or show explanation
            return
        }

        // Get emergency numbers for the current location
        val emergencyNumbers = telephonyManager.emergencyNumbers
        val callIntent = Intent(Intent.ACTION_CALL)

        if (emergencyNumbers.isNotEmpty()) {
            // Use the first emergency number from the list
            callIntent.data = Uri.parse("tel:${emergencyNumbers[0].number}")
        } else {
            // Fallback to 911 if no emergency numbers are available
            callIntent.data = Uri.parse("tel:911")
        }

        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(callIntent)
    }

    private fun startSosLocationUpdates() {
        // Start more frequent location updates during SOS (every 30 seconds)
        if (sosLocationJob != null) {
            // Already running
            return
        }

        sosLocationJob = CoroutineScope(Dispatchers.IO).launch {
            while (isSosActive) {
                try {
                    // Get last known location
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED) {
                        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (lastKnownLocation != null) {
                            // Save location with SOS flag or to a special SOS location table
                            saveSosLocation(lastKnownLocation)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Wait 30 seconds before next update during SOS
                delay(30 * 1000)
            }
        }
    }

    
    private fun buildSosNotification(): Notification {
        val intent = Intent(this, HomeScreenActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        return NotificationCompat.Builder(this, SOS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.sos_active))
            .setContentText(getString(R.string.sos_active_description))
            .setSmallIcon(R.drawable.ic_error_outline_24)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setSound(alarmUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SOS_NOTIFICATION_CHANNEL_ID,
                getString(R.string.sos_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = getString(R.string.sos_notification_channel_description)
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setShowBadge(true)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        deactivateSos()
    }
}
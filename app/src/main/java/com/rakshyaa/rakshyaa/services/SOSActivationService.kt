package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinlinenumberassigned
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

        // Save incident to Supabase
        saveSosIncident(isFalseAlarm)

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
        coroutineScope = null
        sosJob = None
    }

    private fun saveSosIncident(isFalseAlarm: Boolean) {
        val userId = securePreferences.getUserId()
        if (userId.isNullOrBlank()) {
            // User not logged in
            return
        }

        coroutineScope = CoroutineScope(Dispatchers.Main)
        sosJob = coroutineScope?.launch {
            try {
                val incidentData = mapOf(
                    "user_id" to userId,
                    "is_false_alarm" to isFalseAlarm,
                    "status" to "active",
                    "activated_at" to sosStartTime,
                    "created_at" to System.currentTimeMillis()
                )

                val response = incidentRepository.createIncident(incidentData)
                // In a real implementation, we would use the incident ID for tracking
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

        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:911") // Emergency number - in real app, this would be configurable
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(callIntent)
    }

    private fun startSosLocationUpdates() {
        // In a real implementation, this would integrate with LocationTrackingService
        #TODO: Implement more frequent location updates during SOS
    }

    private fun notifyAdminPortal() {
        // In a real implementation, this would trigger a Supabase edge function
        #TODO: Implement admin portal notification via Supabase real-time or edge function
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
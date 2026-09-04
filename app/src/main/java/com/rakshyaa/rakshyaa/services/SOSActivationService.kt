package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.repositories.IncidentRepository
import com.rakshyaa.rakshyaa.data.repositories.LocationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that drives an SOS emergency: periodically records the user's
 * location (flagged as SOS), reports the incident to the backend for the admin
 * portal, and triggers an emergency call when appropriate.
 */
@AndroidEntryPoint
class SOSActivationService : Service() {

    @Inject lateinit var securePreferences: SecurePreferences
    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var locationRepository: LocationRepository

    companion object {
        private const val SOS_NOTIFICATION_CHANNEL_ID = "sos_notification_channel"
        private const val SOS_NOTIFICATION_ID = 2

        const val ACTION_ACTIVATE_SOS = "ACTION_ACTIVATE_SOS"
        const val ACTION_DEACTIVATE_SOS = "ACTION_DEACTIVATE_SOS"
        const val EXTRA_IS_FALSE_ALARM = "extra_is_false_alarm"
    }

    private var isSosActive = false
    private var currentIncidentId: String? = null
    private var locationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ACTIVATE_SOS ->
                activateSos(intent.getBooleanExtra(EXTRA_IS_FALSE_ALARM, false))
            ACTION_DEACTIVATE_SOS -> deactivateSos()
        }
        return START_STICKY
    }

    private fun activateSos(isFalseAlarm: Boolean) {
        if (isSosActive) return
        isSosActive = true
        createNotificationChannel()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    SOS_NOTIFICATION_ID,
                    buildSosNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                )
            } else {
                startForeground(SOS_NOTIFICATION_ID, buildSosNotification())
            }
        } catch (e: Exception) {
            Log.e("SOSActivationService", "Failed to start foreground, retrying without full-screen intent", e)
            try {
                val fallbackNotification = buildSosNotification(useFullScreenIntent = false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        SOS_NOTIFICATION_ID,
                        fallbackNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(SOS_NOTIFICATION_ID, fallbackNotification)
                }
            } catch (e2: Exception) {
                Log.e("SOSActivationService", "Failed to start foreground service", e2)
                stopSelf()
                return
            }
        }

        val lastKnown = lastKnownLocation()
        scope.launch {
            val incident = incidentRepository.create(
                latitude = lastKnown?.latitude,
                longitude = lastKnown?.longitude,
                note = if (isFalseAlarm) "false_alarm" else ""
            )
            currentIncidentId = incident.id
        }

        if (!isFalseAlarm) makeEmergencyCall()
        startSosLocationUpdates()
    }

    private fun deactivateSos() {
        if (!isSosActive) return
        isSosActive = false
        locationJob?.cancel()
        locationJob = null
        currentIncidentId?.let { id ->
            scope.launch { incidentRepository.resolve(id) }
        }
        currentIncidentId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSosLocationUpdates() {
        if (locationJob != null) return
        locationJob = scope.launch {
            while (isSosActive) {
                lastKnownLocation()?.let { loc ->
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        runCatching {
                            locationRepository.saveSosLocation(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy
                            )
                        }
                    }
                }
                delay(30_000L)
            }
        }
    }

    private fun lastKnownLocation(): Location? {
        val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return runCatching {
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
    }

    private fun makeEmergencyCall() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:112"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(callIntent)
        }
    }

    private fun buildSosNotification(useFullScreenIntent: Boolean = true): Notification {
        val intent = Intent(this, com.rakshyaa.rakshyaa.ui.MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val builder = NotificationCompat.Builder(this, SOS_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.sos_active_title))
            .setContentText(getString(R.string.sos_active_description))
            .setSmallIcon(R.drawable.ic_error_outline_24)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmUri)
        if (useFullScreenIntent) {
            builder.setFullScreenIntent(pendingIntent, true)
        }
        return builder.build()
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
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

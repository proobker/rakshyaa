package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.FakeCallRepository
import dagger.hilt.android.AndroidEntryPoint
import hiltService
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinlinenumberassigned

/**
 * Service for simulating incoming calls as an escape aid
 */
@AndroidEntryPoint
@hiltService
class FakeCallService @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferences: SecurePreferences,
    private val fakeCallRepository: FakeCallRepository
) : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "fake_call_channel"
        private const val NOTIFICATION_ID = 8
        const val ACTION_START_FAKE_CALL = "ACTION_START_FAKE_CALL"
        const val ACTION_STOP_FAKE_CALL = "ACTION_STOP_FAKE_CALL"
        const val ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
        const val ACTION_TOGGLE_MUTE = "ACTION_TOGGLE_MUTE"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_CALLER_PHOTO = "extra_caller_photo" // URI or resource ID
        const val EXTRA_CALL_DURATION_S = "extra_call_duration_s"
        const val EXTRA_IS_VIDEO_CALL = "extra_is_video_call"
        private const val RINGTONE_URI = "content://settings/system/ringtone"
    }

    private var coroutineScope: CoroutineScope? = null
    private var callJob: Job? = null
    private var isServiceActive = false
    private var isCallActive = false
    private var isCallConnected = false
    private var isMuted = false

    // Call data
    private var callerName: String = "Unknown Caller"
    private var callerNumber: String = "Unknown"
    private var callerPhotoUri: Uri? = null
    private var callDurationS: Int = 30 // Default 30 seconds
    private var isVideoCall: Boolean = false
    private var callStartTime: Long = 0
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FAKE_CALL -> {
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Mom"
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "555-1234"
                val callerPhotoUriStr = intent.getStringExtra(EXTRA_CALLER_PHOTO)
                val callerPhotoUri = if (callerPhotoUriStr != null) Uri.parse(callerPhotoUriStr) else null
                val callDurationS = intent.getIntExtra(EXTRA_CALL_DURATION_S, 30)
                val isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO_CALL, false)
                startFakeCall(callerName, callerNumber, callerPhotoUri, callDurationS, isVideoCall)
            }
            ACTION_STOP_FAKE_CALL -> stopFakeCall()
            ACTION_ANSWER_CALL -> answerCall()
            ACTION_REJECT_CALL -> rejectCall()
            ACTION_TOGGLE_MUTE -> toggleMute()
        }
        return START_STICKY
    }

    private fun startFakeCall(
        callerName: String,
        callerNumber: String,
        callerPhotoUri: Uri?,
        callDurationS: Int,
        isVideoCall: Boolean
    ) {
        if (isCallActive) {
            // If a call is already active, stop it first
            stopFakeCall()
        }

        this.callerName = callerName
        this.callerNumber = callerNumber
        this.callerPhotoUri = callerPhotoUri
        this.callDurationS = callDurationS
        this.isVideoCall = isVideoCall
        isCallActive = true
        isCallConnected = false
        callStartTime = System.currentTimeMillis()

        // Start foreground service to keep the call active
        startForeground(NOTIFICATION_ID, buildCallNotification("Incoming Call...", R.drawable.ic_phone_in_talk_24))

        // Set up coroutine scope for call logic
        coroutineScope = CoroutineScope(Dispatchers.Main)
        callJob = coroutineScope?.launch {
            #TODO: In a real implementation, we would show an incoming call UI
            #For now, we'll simulate the call with notifications and audio

            // Start playing ringtone
            startRingtone()

            #TODO: Save the fake call record to the database
            #fakeCallRepository.logFakeCall(
            #    callerName = callerName,
            #    callerNumber = callerNumber,
            #    startTime = callStartTime,
            #    isVideoCall = isVideoCall
            #)

            // Auto-end the call after the specified duration
            delay(TimeUnit.SECONDS.toMillis(callDurationS))
            if (isCallActive) {
                endCall()
            }
        }

        // Show the incoming call notification
        showIncomingCallNotification()
    }

    private fun stopFakeCall() {
        if (!isCallActive) return
        isCallActive = false
        isCallConnected = false

        // Stop any ongoing audio
        stopRingtone()
        stopCallAudio()

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        callJob?.cancel()
        coroutineScope = null
        callJob = null

        // Save call record to database
        val userId = securePreferences.getUserId()
        if (userId.isNotBlank()) {
            coroutineScope?.launch {
                try {
                    fakeCallRepository.logFakeCall(
                        userId = userId,
                        callerName = callerName,
                        callerNumber = callerNumber,
                        startTime = callStartTime,
                        endTime = System.currentTimeMillis(),
                        isVideoCall = isVideoCall,
                        wasConnected = isCallConnected
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Reset call data
        callerName = "Unknown Caller"
        callerNumber = "Unknown"
        callerPhotoUri = null
        callDurationS = 30
        isVideoCall = false
    }

    private fun answerCall() {
        if (!isCallActive || isCallConnected) return
        isCallConnected = true

        #TODO: In a real implementation, we would transition to an active call UI
        #For now, we'll just update the notification

        stopRingtone() // Stop the ringtone
        startCallAudio() // Start call audio (could be background conversation audio)

        updateCallNotification("Call Connected", R.drawable.ic_phone_callback_24)

        #TODO: Save call connection to database
        #fakeCallRepository.updateFakeCallConnection(
        #    callerName = callerName,
        #    callerNumber = callerNumber,
        #    connectedAt = System.currentTimeMillis()
        #)
    }

    private fun rejectCall() {
        if (!isCallActive) return
        endCall()
    }

    private fun toggleMute() {
        isMuted = !isMuted
        #TODO: Implement actual muting of call audio
        #For now, just log it
        android.util.Log.i("FakeCallService", "Call${if (isMuted) " muted" else " unmuted"}")
    }

    private fun endCall() {
        isCallActive = false
        isCallConnected = false

        // Stop any ongoing audio
        stopRingtone()
        stopCallAudio()

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        // Clean up coroutine scope
        callJob?.cancel()
        coroutineScope = null
        callJob = null

        // Save call record to database
        val userId = securePreferences.getUserId()
        if (userId.isNotBlank()) {
            coroutineScope?.launch {
                try {
                    fakeCallRepository.logFakeCall(
                        userId = userId,
                        callerName = callerName,
                        callerNumber = callerNumber,
                        startTime = callStartTime,
                        endTime = System.currentTimeMillis(),
                        isVideoCall = isVideoCall,
                        wasConnected = isCallConnected
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Show call ended notification
        showNotification("Call Ended", "The call has ended.", R.drawable.ic_phone_callback_24)
    }

    private fun startRingtone() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setLooping(true)
                // In a real app, you might use a custom ringtone or the default ringtone
                setDataSource(context, Uri.parse(RINGTONE_URI))
                prepare()
                start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            #TODO: Fallback to a system sound or vibration
        }
    }

    private fun stopRingtone() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startCallAudio() {
        #TODO: Implement call audio (could be background conversation audio to make it realistic)
        #For now, we'll just log it
        android.util.Log.i("FakeCallService", "Call audio started")
    }

    private fun stopCallAudio() {
        #TODO: Implement stopping call audio
        android.util.Log.i("FakeCallService", "Call audio stopped")
    }

    private fun showIncomingCallNotification() {
        val answerIntent = Intent(this, FakeCallService::class.java).apply {
            action = ACTION_ANSWER_CALL
        }
        val answerPendingIntent = PendingIntent.getService(
            this,
            1,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, FakeCallService::class.java).apply {
            action = ACTION_REJECT_CALL
        }
        val rejectPendingIntent = PendingIntent.getService(
            this,
            2,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, FakeCallService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            3,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_phone_in_talk_24)
            .setContentTitle("Incoming Call")
            .setContentText("$callerName\n$callerNumber")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(R.drawable.ic_phone_callback_24, "Answer", answerPendingIntent)
            .addAction(R.drawable.ic_call_end_24, "Reject", rejectPendingIntent)
            .addAction(R.drawable.ic_volume_off_24, "Mute", mutePendingIntent)
            .setFullScreenIntent(answerPendingIntent, true)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateCallNotification(contentText: String, iconId: Int) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(iconId)
            .setContentTitle("Fake Call Active")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(contentText: String, iconId: Int): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.fake_call_service_active))
            .setContentText(contentText)
            .setSmallIcon(iconId)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.fake_call_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = getString(R.string.fake_call_channel_description)
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, text: String, iconId: Int) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(iconId)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(997, notification) // Use a different ID for non-ongoing notifications
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not a bound service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFakeCall()
    }
}
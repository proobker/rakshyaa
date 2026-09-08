package com.rakshyaa.rakshyaa.services

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class OngoingFakeCall(
    val callerName: String,
    val callerNumber: String,
    val startedAt: Long,
    val connected: Boolean,
    val isVideo: Boolean
)

@Singleton
class FakeCallService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var current: OngoingFakeCall? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun startCall(
        callerName: String,
        callerNumber: String,
        isVideo: Boolean = false
    ): OngoingFakeCall {
        current = OngoingFakeCall(
            callerName = callerName,
            callerNumber = callerNumber,
            startedAt = System.currentTimeMillis(),
            connected = false,
            isVideo = isVideo
        )
        return current!!
    }

    fun answerCall(): OngoingFakeCall? {
        val call = current ?: return null
        current = call.copy(connected = true)
        stopRingtone()
        return current
    }

    fun endCall(): OngoingFakeCall? {
        stopRingtone()
        val ended = current
        current = null
        return ended
    }

    fun currentCall(): OngoingFakeCall? = current

    fun startRingtone() {
        stopRingtone()
        try {
            val ringtoneUri = Settings.System.DEFAULT_RINGTONE_URI
            mediaPlayer = MediaPlayer().apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(audioAttributes)
                setDataSource(context, ringtoneUri)
                setLooping(true)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        startVibration(context)
    }

    fun stopRingtone() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) { }
        mediaPlayer = null
        stopVibration()
    }

    private fun startVibration(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 1000, 500), 0)
        )
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    fun cleanup() {
        stopRingtone()
        current = null
    }
}

package com.rakshyaa.rakshyaa.services

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
class FakeCallService @Inject constructor() {

    private var current: OngoingFakeCall? = null

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
        return current
    }

    fun endCall(): OngoingFakeCall? {
        val ended = current
        current = null
        return ended
    }

    fun currentCall(): OngoingFakeCall? = current
}
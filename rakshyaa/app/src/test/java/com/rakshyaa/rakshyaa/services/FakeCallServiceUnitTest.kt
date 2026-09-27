package com.rakshyaa.rakshyaa.services

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FakeCallServiceUnitTest {

    private lateinit var fakeCallService: FakeCallService

    @Before
    fun setUp() {
        fakeCallService = FakeCallService(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `startCall creates an incoming call and exposes it as current`() {
        val call = fakeCallService.startCall("Mom", "+1-555-0123", isVideo = false)

        assertThat(call.callerName).isEqualTo("Mom")
        assertThat(call.callerNumber).isEqualTo("+1-555-0123")
        assertThat(call.connected).isFalse()
        assertThat(fakeCallService.currentCall()).isSameInstanceAs(call)
    }

    @Test
    fun `answerCall marks the ongoing call as connected and stops the ringtone`() {
        fakeCallService.startCall("Mom", "+1-555-0123")

        val answered = fakeCallService.answerCall()

        assertThat(answered).isNotNull()
        assertThat(answered!!.connected).isTrue()
        assertThat(fakeCallService.currentCall()!!.connected).isTrue()
    }

    @Test
    fun `answerCall without an ongoing call returns null`() {
        assertThat(fakeCallService.answerCall()).isNull()
    }

    @Test
    fun `endCall clears the current call`() {
        fakeCallService.startCall("Mom", "+1-555-0123")

        val ended = fakeCallService.endCall()

        assertThat(ended).isNotNull()
        assertThat(fakeCallService.currentCall()).isNull()
    }

    @Test
    fun `cleanup releases ringtone and current call`() {
        fakeCallService.startCall("Mom", "+1-555-0123")
        fakeCallService.startRingtone()

        fakeCallService.cleanup()

        assertThat(fakeCallService.currentCall()).isNull()
    }
}
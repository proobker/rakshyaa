package com.rakshyaa.rakshyaa.ui

import android.content.Intent
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.rakshyaa.rakshyaa.ui.components.emergencyMessageIntent
import com.rakshyaa.rakshyaa.ui.components.emergencyDialIntent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmergencyIntentsTest {
    @Test fun `SMS is composed for distinct valid recipients without sending`() {
        val intent = emergencyMessageIntent(listOf("+977 9812345678", "+9779812345678", "bad"), "Please help")
        assertThat(intent.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(intent.data?.scheme).isEqualTo("smsto")
        assertThat(Uri.decode(intent.data?.encodedSchemeSpecificPart)).isEqualTo("+9779812345678")
        assertThat(intent.getStringExtra("sms_body")).isEqualTo("Please help")
    }
    @Test fun `dialer does not assume an emergency number or place a call`() {
        val intent = emergencyDialIntent()
        assertThat(intent.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(intent.data).isNull()
    }
}

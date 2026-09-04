package com.rakshyaa.rakshyaa.services

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import com.google.common.truth.Truth.assertThat

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class SOSActivationServiceUnitTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `onStartCommand with DEACTIVATE does nothing when not active`() {
        val serviceController = Robolectric.buildService(
            SOSActivationService::class.java
        )
        val service = serviceController.get()
        val intent = Intent(ApplicationProvider.getApplicationContext(), SOSActivationService::class.java).apply {
            action = SOSActivationService.ACTION_DEACTIVATE_SOS
        }
        serviceController.startCommand(intent, 0)
        val nm = org.robolectric.RuntimeEnvironment.getApplication()
            .getSystemService(android.app.NotificationManager::class.java) as ShadowNotificationManager
        assertThat(nm.allNotifications).isEmpty()
        serviceController.destroy()
    }

    @Test
    fun `companion constants are defined`() {
        assertThat(SOSActivationService.ACTION_ACTIVATE_SOS).isEqualTo("ACTION_ACTIVATE_SOS")
        assertThat(SOSActivationService.ACTION_DEACTIVATE_SOS).isEqualTo("ACTION_DEACTIVATE_SOS")
        assertThat(SOSActivationService.EXTRA_IS_FALSE_ALARM).isEqualTo("extra_is_false_alarm")
    }
}

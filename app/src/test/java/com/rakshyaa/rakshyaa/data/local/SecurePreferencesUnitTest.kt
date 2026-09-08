package com.rakshyaa.rakshyaa.data.local

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
class SecurePreferencesUnitTest {

    private lateinit var securePreferences: SecurePreferences

    @Before
    fun setUp() {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("secure_prefs_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        securePreferences = SecurePreferences(prefs)
    }

    @Test
    fun `saveAccessToken should store token in shared preferences`() {
        securePreferences.saveAccessToken("test-access-token")

        assertThat(securePreferences.getAccessToken()).isEqualTo("test-access-token")
    }

    @Test
    fun `getAccessToken returns null when not set`() {
        securePreferences.clear()

        assertThat(securePreferences.getAccessToken()).isNull()
    }

    @Test
    fun `saveRefreshToken should store token in shared preferences`() {
        securePreferences.saveRefreshToken("test-refresh-token")

        assertThat(securePreferences.getRefreshToken()).isEqualTo("test-refresh-token")
    }

    @Test
    fun `saveUserId should store userId in shared preferences`() {
        securePreferences.saveUserId("test-user-id")

        assertThat(securePreferences.getUserId()).isEqualTo("test-user-id")
    }

    @Test
    fun `saveUserEmail should store email in shared preferences`() {
        securePreferences.saveUserEmail("test@example.com")

        assertThat(securePreferences.getUserEmail()).isEqualTo("test@example.com")
    }

    @Test
    fun `saveLoginState should store login state`() {
        securePreferences.saveLoginState(true)

        assertThat(securePreferences.isLoggedIn()).isTrue()
    }

    @Test
    fun `isLoggedIn returns false when not set`() {
        securePreferences.clear()

        assertThat(securePreferences.isLoggedIn()).isFalse()
    }

    @Test
    fun `clear should remove all preferences`() {
        securePreferences.saveAccessToken("token")
        securePreferences.saveLoginState(true)

        securePreferences.clear()

        assertThat(securePreferences.getAccessToken()).isNull()
        assertThat(securePreferences.isLoggedIn()).isFalse()
    }

    @Test
    fun `saveAuthCredentials should store all credentials and mark signed in`() {
        securePreferences.saveAuthCredentials(
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token",
            userId = "test-user-id",
            userEmail = "test@example.com"
        )

        assertThat(securePreferences.getAccessToken()).isEqualTo("test-access-token")
        assertThat(securePreferences.getRefreshToken()).isEqualTo("test-refresh-token")
        assertThat(securePreferences.getUserId()).isEqualTo("test-user-id")
        assertThat(securePreferences.getUserEmail()).isEqualTo("test@example.com")
        assertThat(securePreferences.isLoggedIn()).isTrue()
    }
}
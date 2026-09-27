package com.rakshyaa.rakshyaa.data.auth

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.network.ApiClient
import org.junit.Test
import org.mockito.Mockito.*

class LocalSessionTest {
    @Test fun `local session requires no network or Google account`() {
        val google = mock(GoogleAuthClient::class.java)
        val api = mock(ApiClient::class.java)
        val prefs = mock(SecurePreferences::class.java)
        val repo = AuthRepository(google, api, prefs, mock(Context::class.java))
        repo.continueOnDevice()
        assertThat(repo.state.value.isLoggedIn).isTrue()
        assertThat(repo.state.value.user?.sub).isEqualTo(AuthRepository.LOCAL_USER)
        verify(prefs).saveUserId(AuthRepository.LOCAL_USER)
        verify(prefs).saveLoginState(true)
        verifyNoInteractions(google, api)
    }

    @Test fun `local session restores without a bearer token`() {
        val prefs = mock(SecurePreferences::class.java)
        `when`(prefs.isLoggedIn()).thenReturn(true)
        `when`(prefs.getUserId()).thenReturn(AuthRepository.LOCAL_USER)
        val repo = AuthRepository(mock(GoogleAuthClient::class.java), mock(ApiClient::class.java),
            prefs, mock(Context::class.java))
        assertThat(repo.state.value.isLoggedIn).isTrue()
        assertThat(repo.state.value.user?.sub).isEqualTo(AuthRepository.LOCAL_USER)
    }
}

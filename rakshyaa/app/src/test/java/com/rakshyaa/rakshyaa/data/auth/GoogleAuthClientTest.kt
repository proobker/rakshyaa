package com.rakshyaa.rakshyaa.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GoogleAuthClientTest {
    @Test
    fun `selector receives foreground activity and explicit Google button request`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            var selectorCalled = false
            val manager = Mockito.mock(CredentialManager::class.java) { invocation ->
                if (invocation.method.name == "getCredential") {
                    selectorCalled = true
                    assertThat(invocation.arguments[0]).isSameInstanceAs(activity)
                    val request = invocation.arguments[1] as GetCredentialRequest
                    assertThat(request.credentialOptions.single()).isInstanceOf(GetSignInWithGoogleOption::class.java)
                    GetCredentialResponse(GoogleIdTokenCredential.Builder()
                        .setId("test@example.com").setIdToken("test-id-token").build())
                } else Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
            assertThat(GoogleAuthClient(manager).getGoogleIdToken(activity)).isEqualTo("test-id-token")
            assertThat(selectorCalled).isTrue()
        } finally {
            controller.pause().stop().destroy()
            Dispatchers.resetMain()
        }
    }
}

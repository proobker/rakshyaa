package com.rakshyaa.rakshyaa.data.auth

import android.content.Context
import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rakshyaa.rakshyaa.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Obtains a Google ID token via Credential Manager (Google Identity Services).
 * The returned ID token is sent to our backend for verification.
 */
@Singleton
class GoogleAuthClient internal constructor(
    private val credentialManager: CredentialManager
) {
    @Inject constructor(@ApplicationContext context: Context) : this(CredentialManager.create(context))

    /** Launches the Google sign-in sheet and returns a Google ID token. */
    suspend fun getGoogleIdToken(activity: Activity): String = withContext(Dispatchers.Main) {
        val googleIdOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(activity, request)
        handleCredential(response.credential)
    }

    private fun handleCredential(credential: Credential): String =
        when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            }
            else -> throw Exception("Unexpected credential type: ${credential.type}")
        }
}

package com.rakshyaa.rakshyaa.data.auth

import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.GoogleAuthRequest
import com.rakshyaa.rakshyaa.data.network.GoogleAuthResponse
import com.rakshyaa.rakshyaa.data.network.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class AuthState(
    val isLoggedIn: Boolean = false,
    val user: UserDto? = null,
    val inProgress: Boolean = false,
    val error: String? = null
)

/**
 * Manages the authentication lifecycle: Google ID token -> backend verification ->
 * session JWT stored securely. Exposes a StateFlow for the UI.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val googleAuthClient: GoogleAuthClient,
    private val apiClient: ApiClient,
    private val securePreferences: SecurePreferences,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        restoreSession()
    }

    /** Restores a previously stored session at startup, if one exists. */
    private fun restoreSession() {
        val loggedIn = securePreferences.isLoggedIn()
        val token = securePreferences.getAccessToken()
        if (loggedIn && securePreferences.getUserId() == LOCAL_USER) {
            _state.value = AuthState(isLoggedIn = true, user = UserDto(sub = LOCAL_USER, name = "On this device"))
        } else if (loggedIn && !token.isNullOrEmpty()) {
            _state.value = AuthState(isLoggedIn = true)
        }
    }

    fun continueOnDevice() {
        securePreferences.clear()
        securePreferences.saveUserId(LOCAL_USER)
        securePreferences.saveLoginState(true)
        _state.value = AuthState(isLoggedIn = true, user = UserDto(sub = LOCAL_USER, name = "On this device"))
    }

    companion object { const val LOCAL_USER = "local-device" }

    suspend fun signInWithGoogle() {
        if (_state.value.inProgress) return
        _state.value = _state.value.copy(inProgress = true, error = null)
        try {
            val idToken = googleAuthClient.getGoogleIdToken()
            val body = json.encodeToString(GoogleAuthRequest.serializer(), GoogleAuthRequest(idToken))
            val responseBody = apiClient.postJsonPublic("/auth/google", body)
            val response = json.decodeFromString(GoogleAuthResponse.serializer(), responseBody)

            securePreferences.saveAuthCredentials(
                accessToken = response.token,
                userId = response.user.sub,
                userEmail = response.user.email
            )
            _state.value = AuthState(isLoggedIn = true, user = response.user)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message ?: "Sign-in failed")
        } finally {
            _state.value = _state.value.copy(inProgress = false)
        }
    }

    suspend fun fetchMe() {
        val current = _state.value
        if (!current.isLoggedIn || securePreferences.getUserId() == LOCAL_USER) return
        try {
            val body = apiClient.get("/backup/me")
            val me = json.decodeFromString<com.rakshyaa.rakshyaa.data.network.MeResponse>(body)
            _state.value = _state.value.copy(user = me.user)
        } catch (_: Exception) {
            // Non-fatal; user can retry.
        }
    }

    suspend fun signOut() {
        listOf(
            com.rakshyaa.rakshyaa.services.SOSActivationService::class.java,
            com.rakshyaa.rakshyaa.services.LocationTrackingService::class.java,
            com.rakshyaa.rakshyaa.services.RideMonitoringService::class.java,
            com.rakshyaa.rakshyaa.services.CheckInService::class.java
        ).forEach { context.stopService(android.content.Intent(context, it)) }
        securePreferences.clear()
        _state.value = AuthState()
    }
}

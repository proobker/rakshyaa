package com.rakshyaa.rakshyaa.data.auth

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import io.github.jmnarloch.supabase.kaft.AuthChangeEvent
import io.github.jmnarloch.supabase.kaft.AuthState
import io.github.jmnarloch.supabase.kaft.PostgrestException
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import io.github.jmnarloch.supabase.kaft.goTrueApi.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository handling all authentication operations using Supabase Auth
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val securePreferences: SecurePreferences
) {

    // Flow to emit auth state changes
    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    init {
        // Listen to auth state changes
        supabaseClient.authService.authStateChanges { _, session ->
            _authState.update { session }

            // Save tokens to secure preferences when session changes
            session?.let {
                securePreferences.saveAuthCredentials(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken,
                    userId = it.user.id,
                    userEmail = it.user.email
                )
            } ?: run {
                // Clear credentials when signing out
                securePreferences.clear()
            }
        }
    }

    /**
     * Sign in user with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<Session> {
        return try {
            val response = supabaseClient.authService.signInWithEmail(email, password)
            Result.success(response.data)
        } catch (e: PostgrestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign up new user with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<Session> {
        return try {
            val response = supabaseClient.authService.signUpWithEmail(email, password)
            Result.success(response.data)
        } catch (e: PostgrestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.authService.signOut()
            Result.success(Unit)
        } catch (e: PostgrestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            supabaseClient.authService.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: PostgrestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current session
     */
    val currentSession: Session?
        get() = supabaseClient.authService.currentSession

    /**
     * Check if user is currently signed in
     */
    val isAuthenticated: Boolean
        get() = currentSession != null

    /**
     * Refresh current session
     */
    suspend fun refreshSession(): Result<Session> {
        return try {
            val response = supabaseClient.authService.refreshCurrentSession()
            Result.success(response.data)
        } catch (e: PostgrestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user from current session
     */
    val currentUser: io.github.jmnarloch.supabase.kaft.goTrueApi.User?
        get() = currentSession?.user

    /**
     * Get access token from secure storage
     */
    fun getAccessToken(): String? {
        return securePreferences.getAccessToken()
    }

    /**
     * Get refresh token from secure storage
     */
    fun getRefreshToken(): String? {
        return securePreferences.getRefreshToken()
    }
}
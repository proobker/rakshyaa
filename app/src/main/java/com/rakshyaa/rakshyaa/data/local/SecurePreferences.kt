package com.rakshyaa.rakshyaa.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.isInstanceOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure preferences handler using EncryptedSharedPreferences
 * Stores sensitive data like authentication tokens securely
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    private val encryptedSharedPreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save access token
     */
    fun saveAccessToken(token: String) {
        encryptedSharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    /**
     * Get access token
     */
    fun getAccessToken(): String? {
        return encryptedSharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Save refresh token
     */
    fun saveRefreshToken(token: String) {
        encryptedSharedPreferences.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply()
    }

    /**
     * Get refresh token
     */
    fun getRefreshToken(): String? {
        return encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Save user ID
     */
    fun saveUserId(userId: String) {
        encryptedSharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Get user ID
     */
    fun getUserId(): String? {
        return encryptedSharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Save user email
     */
    fun saveUserEmail(email: String) {
        encryptedSharedPreferences.edit()
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? {
        return encryptedSharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Save login state
     */
    fun saveLoginState(isLoggedIn: Boolean) {
        encryptedSharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply()
    }

    /**
     * Get login state
     */
    fun isLoggedIn(): Boolean {
        return encryptedSharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Clear all secure preferences (logout)
     */
    fun clear() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    /**
     * Save multiple tokens at once (useful for login)
     */
    fun saveAuthCredentials(
        accessToken: String,
        refreshToken: String? = null,
        userId: String? = null,
        userEmail: String? = null
    ) {
        edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            userId?.let { putString(KEY_USER_ID, it) }
            userEmail?.let { putString(KEY_USER_EMAIL, it) }
            putBoolean(KEY_IS_LOGGED_IN, true)
        }
    }

    /**
     * Edit helper for secure preferences
     */
    private fun edit(action: SharedPreferences.Editor.() -> Unit) {
        encryptedSharedPreferences.edit().apply(action).apply()
    }
}
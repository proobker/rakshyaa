package com.rakshyaa.rakshyaa.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure preferences handler using EncryptedSharedPreferences.
 * Stores sensitive data like authentication tokens securely.
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
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAccessToken(token: String) {
        encryptedSharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? =
        encryptedSharedPreferences.getString(KEY_ACCESS_TOKEN, null)

    fun saveRefreshToken(token: String) {
        encryptedSharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? =
        encryptedSharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    fun saveUserId(userId: String) {
        encryptedSharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? =
        encryptedSharedPreferences.getString(KEY_USER_ID, null)

    fun saveUserEmail(email: String) {
        encryptedSharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? =
        encryptedSharedPreferences.getString(KEY_USER_EMAIL, null)

    fun saveLoginState(isLoggedIn: Boolean) {
        encryptedSharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean =
        encryptedSharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clear() {
        encryptedSharedPreferences.edit().clear().apply()
    }

    fun saveAuthCredentials(
        accessToken: String,
        refreshToken: String? = null,
        userId: String? = null,
        userEmail: String? = null
    ) {
        val editor = encryptedSharedPreferences.edit()
        editor.putString(KEY_ACCESS_TOKEN, accessToken)
        refreshToken?.let { editor.putString(KEY_REFRESH_TOKEN, it) }
        userId?.let { editor.putString(KEY_USER_ID, it) }
        userEmail?.let { editor.putString(KEY_USER_EMAIL, it) }
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }
}

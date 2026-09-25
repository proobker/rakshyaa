package com.rakshyaa.rakshyaa.data.auth

import android.app.ActivityManager
import android.content.Context
import com.rakshyaa.rakshyaa.data.network.ApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountDeletion @Inject constructor(
    private val api: ApiClient,
    private val auth: AuthRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun delete() {
        if (auth.state.value.user?.sub != AuthRepository.LOCAL_USER) api.deleteAccount()
        // Android clears the app's private files, preferences, caches and running services.
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        check(manager.clearApplicationUserData()) {
            "Account deleted. Clear Rakshyaa storage in Android settings to remove this device's remaining files."
        }
        auth.signOut()
    }
}

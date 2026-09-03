package com.rakshyaa.rakshyaa

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class RakshyaaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid configuration
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID)
    }
}

package com.rakshyaa.rakshyaa.services

import android.content.Context
import android.location.Address
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a human-friendly place name (shop, monument, locality) from coordinates
 * using the Android SDK Geocoder. Runs off the main thread and returns null when
 * the device cannot resolve an address (e.g. geocoding unavailable).
 */
@Singleton
class GeocodingService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                Geocoder(context).getFromLocation(latitude, longitude, 1)
            }.getOrNull()?.firstOrNull()?.toPlaceLabel()
        }

    private fun Address.toPlaceLabel(): String? {
        val name = featureName?.takeIf { it.isNotBlank() && it != "Unnamed" }
        val thoroughfare = thoroughfare?.takeIf { it.isNotBlank() }
        val locality = locality?.takeIf { it.isNotBlank() }
        val subAdminArea = subAdminArea?.takeIf { it.isNotBlank() }
        val adminArea = adminArea?.takeIf { it.isNotBlank() }

        return when {
            name != null && locality != null -> "$name, $locality"
            name != null -> name
            thoroughfare != null && locality != null -> "$thoroughfare, $locality"
            locality != null -> locality
            subAdminArea != null -> subAdminArea
            adminArea != null -> adminArea
            else -> null
        }
    }
}

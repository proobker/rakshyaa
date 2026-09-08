package com.rakshyaa.rakshyaa.data.repositories

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.LocationRecord
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Persists a small encrypted, on-device log of recent user locations. All data is
 * encrypted at rest (keys stay in the Android Keystore) and never leaves the device
 * in plaintext. Used to support SOS, ride monitoring and check-in flows.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val store: EncryptedLocalStore,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(LocationRecord.serializer())
    private val key = "location_logs"

    /** Saves a location update to the encrypted local log. */
    suspend fun saveLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float = 0f,
        timestamp: Long = System.currentTimeMillis(),
        isSos: Boolean = false
    ) {
        val record = LocationRecord(
            id = UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = timestamp,
            isSos = isSos
        )
        val logs = loadAll().toMutableList()
        logs.add(record)
        // Keep only the most recent 500 records to bound storage.
        store.savePlain(key, json.encodeToString(listSerializer, logs.takeLast(500)))
    }

    /** Returns the most recent recorded location, or null if none exists. */
    fun getLastKnownLocation(): LocationRecord? = loadAll().lastOrNull()

    /**
     * Requests a fresh one-shot location fix (balanced accuracy), falling back to
     * the last recorded location on failure or timeout. Returns null if neither
     * is available (e.g. location permission not granted).
     */
    suspend fun currentLocation(): LocationRecord? {
        val fresh = runCatching {
            withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val task = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    )
                    task.addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    task.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
        }.getOrNull()

        val freshRecord = fresh?.let {
            LocationRecord(
                id = UUID.randomUUID().toString(),
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                timestamp = it.time
            )
        }
        return freshRecord ?: getLastKnownLocation()
    }

    /** Returns recorded locations, most recent first. */
    fun getLocationHistory(): List<LocationRecord> = loadAll().asReversed()

    /** Records an SOS location update (flagged with isSos). */
    suspend fun saveSosLocation(
        latitude: Double,
        longitude: Double,
        accuracy: Float = 0f
    ) {
        saveLocation(latitude, longitude, accuracy, isSos = true)
    }

    /** Removes all recorded locations. */
    fun clear() {
        store.delete(key)
    }

    private fun loadAll(): List<LocationRecord> {
        val raw = store.loadPlain(key)
        if (raw == null) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }
            .getOrElse { emptyList() }
    }
}

package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.LocationRecord
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists a small encrypted, on-device log of recent user locations. All data is
 * encrypted at rest (keys stay in the Android Keystore) and never leaves the device
 * in plaintext. Used to support SOS, ride monitoring and check-in flows.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val store: EncryptedLocalStore
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

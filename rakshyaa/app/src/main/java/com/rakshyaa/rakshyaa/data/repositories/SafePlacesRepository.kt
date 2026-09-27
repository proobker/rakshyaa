package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.PlaceDto
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import com.rakshyaa.rakshyaa.utils.GeoUtils
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages safe places (hospitals, clinics, police/fire stations + user-added spots).
 *
 * Nearby discovery is served live by the backend (Overpass / OpenStreetMap). Results are
 * sorted by distance; places within the search radius are "nearby", and when none exist
 * the nearest one beyond the radius is surfaced as the "closest" match. If the network
 * call fails, a small hardcoded set is used as an offline fallback.
 */
@Singleton
class SafePlacesRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager,
    private val apiClient: ApiClient
) : EncryptedListRepository<SafePlace>(
    store = store,
    sync = sync,
    key = "safe_places",
    elementSerializer = SafePlace.serializer()
) {

    /** Result of a nearby lookup: within-radius places plus an optional nearest match
     *  beyond the radius (used when the within-radius list is empty). */
    data class NearbyPlacesResult(
        val nearby: List<SafePlace>,
        val closest: SafePlace?,
        val isLive: Boolean
    )

    suspend fun getAll(): List<SafePlace> {
        val userPlaces = loadAll()
        return DEFAULT_PLACES + userPlaces
    }

    suspend fun nearby(latitude: Double, longitude: Double, radiusM: Double): NearbyPlacesResult {
        val live = runCatching { apiClient.getNearbyPlaces(latitude, longitude) }
            .getOrNull()
            ?.map { it.toSafePlace() }

        if (live != null) {
            val within = live.filter { it.distanceMeters <= radiusM.toLong() }
            val closest = if (within.isEmpty()) live.minByOrNull { it.distanceMeters } else null
            val userWithin = loadAll()
                .filter { GeoUtils.haversineDistance(latitude, longitude, it.latitude, it.longitude) <= radiusM }
                .map { it.distanceFrom(latitude, longitude) }
                .sortedBy { it.distanceMeters }
            return NearbyPlacesResult(
                nearby = within + userWithin,
                closest = closest,
                isLive = true
            )
        }

        val all = (DEFAULT_PLACES + loadAll())
            .map { it.distanceFrom(latitude, longitude) }
            .sortedBy { it.distanceMeters }
        val nearby = all.filter { it.distanceMeters <= radiusM.toLong() }
        val closest = if (nearby.isEmpty()) all.firstOrNull() else null
        return NearbyPlacesResult(
            nearby = nearby,
            closest = closest,
            isLive = false
        )
    }

    /**
     * Same [nearby] cut/closest logic but never touches the network: uses the
     * hardcoded fallback set plus the user's saved places. Used when the user's
     * location is unavailable (permission off), so the screen still shows a generic
     * list instead of a blank state.
     */
    suspend fun offlineFallback(latitude: Double, longitude: Double, radiusM: Double): NearbyPlacesResult {
        val all = (DEFAULT_PLACES + loadAll())
            .map { it.distanceFrom(latitude, longitude) }
            .sortedBy { it.distanceMeters }
        val nearby = all.filter { it.distanceMeters <= radiusM.toLong() }
        val closest = if (nearby.isEmpty()) all.firstOrNull() else null
        return NearbyPlacesResult(
            nearby = nearby,
            closest = closest,
            isLive = false
        )
    }

    suspend fun add(place: SafePlace): SafePlace {
        val withId = place.copy(id = place.id.ifEmpty { UUID.randomUUID().toString() })
        modify { it + withId }
        return withId
    }

    suspend fun remove(id: String) {
        modify { list -> list.filterNot { it.id == id } }
    }

    private fun SafePlace.distanceFrom(lat: Double, lon: Double): SafePlace =
        copy(
            distanceMeters = GeoUtils.haversineDistance(lat, lon, latitude, longitude)
                .toLong()
                .coerceAtLeast(0)
        )

    private fun PlaceDto.toSafePlace(): SafePlace = SafePlace(
        id = id,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        type = type,
        distanceMeters = distanceMeters.coerceAtLeast(0)
    )

    companion object {
        // Offline fallback only — used when the live lookup fails.
        private val DEFAULT_PLACES = listOf(
            SafePlace("hospital-1", "City General Hospital", "Central District", 27.7172, 85.3240, "hospital"),
            SafePlace("police-1", "Central Police Station", "Central District", 27.7052, 85.3269, "police"),
            SafePlace("fire-1", "Central Fire Station", "Central District", 27.7100, 85.3200, "fire"),
            SafePlace("hospital-2", "Community Health Center", "East District", 27.7000, 85.3400, "hospital")
        )
    }
}
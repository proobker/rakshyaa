package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import com.rakshyaa.rakshyaa.utils.GeoUtils
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages safe places (hospitals, police, fire stations + user-added spots).
 * Pre-seeded with a small set of well-known nationwide emergency locations.
 */
@Singleton
class SafePlacesRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager
) : EncryptedListRepository<SafePlace>(
    store = store,
    sync = sync,
    key = "safe_places",
    elementSerializer = SafePlace.serializer()
) {

    suspend fun getAll(): List<SafePlace> {
        val userPlaces = loadAll()
        return DEFAULT_PLACES + userPlaces
    }

    suspend fun nearby(latitude: Double, longitude: Double, radiusM: Double): List<SafePlace> =
        getAll().filter {
            GeoUtils.haversineDistance(latitude, longitude, it.latitude, it.longitude) <= radiusM
        }

    suspend fun add(place: SafePlace): SafePlace {
        val withId = place.copy(id = place.id.ifEmpty { UUID.randomUUID().toString() })
        modify { it + withId }
        return withId
    }

    suspend fun remove(id: String) {
        modify { list -> list.filterNot { it.id == id } }
    }

    companion object {
        private val DEFAULT_PLACES = listOf(
            SafePlace("hospital-1", "City General Hospital", "Central District", 27.7172, 85.3240, "hospital"),
            SafePlace("police-1", "Central Police Station", "Central District", 27.7052, 85.3269, "police"),
            SafePlace("fire-1", "Central Fire Station", "Central District", 27.7100, 85.3200, "fire"),
            SafePlace("hospital-2", "Community Health Center", "East District", 27.7000, 85.3400, "hospital")
        )
    }
}

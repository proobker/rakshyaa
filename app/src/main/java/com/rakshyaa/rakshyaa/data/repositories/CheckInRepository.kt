package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.CheckIn
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages scheduled safety check-ins. Persisted encrypted + cloud-synced.
 */
@Singleton
class CheckInRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager
) : EncryptedListRepository<CheckIn>(
    store = store,
    sync = sync,
    key = "check_ins",
    elementSerializer = CheckIn.serializer()
) {

    suspend fun getAll(): List<CheckIn> = loadAll().sortedBy { it.scheduledAt }

    suspend fun getUpcoming(): List<CheckIn> =
        getAll().filter { it.status == "pending" }

    suspend fun schedule(at: Long): CheckIn {
        val checkIn = CheckIn(id = UUID.randomUUID().toString(), scheduledAt = at)
        modify { it + checkIn }
        return checkIn
    }

    suspend fun complete(id: String, latitude: Double?, longitude: Double?) {
        modify { list ->
            list.map {
                if (it.id == id) it.copy(
                    status = "completed",
                    checkedInAt = System.currentTimeMillis(),
                    latitude = latitude,
                    longitude = longitude
                ) else it
            }
        }
    }

    suspend fun markMissed(id: String) {
        modify { list ->
            list.map { if (it.id == id) it.copy(status = "missed") else it }
        }
    }

    suspend fun escalate(id: String) {
        modify { list ->
            list.map { if (it.id == id) it.copy(status = "escalated") else it }
        }
    }
}

package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.Incident
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.IncidentRequest
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages SOS incidents: stored locally (encrypted) and additionally reported to
 * the backend so an admin portal can surface active emergencies.
 */
@Singleton
class IncidentRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager,
    private val apiClient: ApiClient
) : EncryptedListRepository<Incident>(
    store = store,
    sync = sync,
    key = "incidents",
    elementSerializer = Incident.serializer()
) {

    suspend fun getAll(): List<Incident> = loadAll().sortedByDescending { it.activatedAt }

    suspend fun create(
        latitude: Double?,
        longitude: Double?,
        note: String = ""
    ): Incident {
        val incident = Incident(
            id = UUID.randomUUID().toString(),
            status = "active",
            latitude = latitude,
            longitude = longitude,
            activatedAt = System.currentTimeMillis(),
            note = note
        )
        modify { it + incident }

        // Report to backend so admin can see active emergencies (best-effort).
        runCatching {
            val body = Json.encodeToString(
                IncidentRequest.serializer(),
                IncidentRequest(
                    status = "active",
                    latitude = latitude,
                    longitude = longitude,
                    activatedAt = incident.activatedAt
                )
            )
            apiClient.postJson("/incidents", body)
        }
        return incident
    }

    suspend fun resolve(id: String) {
        modify { list ->
            list.map { if (it.id == id) it.copy(status = "resolved") else it }
        }
        runCatching { apiClient.postJson("/incidents/$id/resolve", "{}") }
    }
}

package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.RideSession
import com.rakshyaa.rakshyaa.data.models.RoutePoint
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import com.rakshyaa.rakshyaa.utils.GeoUtils
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks ride sessions, logging GPS waypoints and detecting route deviation
 * (Haversine distance from the logged route > [MAX_DEVIATION_M]).
 */
@Singleton
class RideRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager
) : EncryptedListRepository<RideSession>(
    store = store,
    sync = sync,
    key = "ride_sessions",
    elementSerializer = RideSession.serializer()
) {

    companion object {
        private const val MAX_DEVIATION_M = 300.0
    }

    suspend fun getAll(): List<RideSession> = loadAll().sortedByDescending { it.startTime }

    suspend fun getActive(): RideSession? = loadAll().firstOrNull { it.endTime == null }

    suspend fun start(): RideSession {
        val session = RideSession(
            id = UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis(),
            waypoints = emptyList(),
            deviated = false
        )
        modify { it + session }
        return session
    }

    suspend fun appendPoint(sessionId: String, latitude: Double, longitude: Double) {
        modify { list ->
            list.map { s ->
                if (s.id != sessionId || s.endTime != null) return@map s
                val newPoint = RoutePoint(latitude, longitude, System.currentTimeMillis())
                val waypoints = s.waypoints + newPoint
                val deviated = detectDeviation(waypoints)
                s.copy(
                    waypoints = waypoints,
                    deviated = deviated,
                    deviationAlertAt = if (deviated && !s.deviated) System.currentTimeMillis() else s.deviationAlertAt
                )
            }
        }
    }

    suspend fun end(sessionId: String) {
        modify { list ->
            list.map { if (it.id == sessionId) it.copy(endTime = System.currentTimeMillis()) else it }
        }
    }

    private fun detectDeviation(waypoints: List<RoutePoint>): Boolean {
        if (waypoints.size < 3) return false
        val last = waypoints.last()
        // Distance from the last point to the nearest earlier waypoint on the route.
        val nearest = waypoints.dropLast(1).minOfOrNull { w ->
            GeoUtils.haversineDistance(w.latitude, w.longitude, last.latitude, last.longitude)
        } ?: 0.0
        return nearest > MAX_DEVIATION_M
    }
}

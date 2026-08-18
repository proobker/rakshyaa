package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import com.rakshyaa.rakshyaa.utils.GeoUtils
import io.github.jmnarloch.supabase.kaft.PostgrestException
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.List

/**
 * Repository for handling ride session data with Supabase
 */
@Singleton
class RideRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    /**
     * Creates a new ride record
     */
    suspend fun createRide(
        userId: String,
        startTime: Long,
        plannedWaypoints: List<android.location.Location>,
        deviationThresholdM: Double
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Convert planned waypoints to storable format
                val waypointsData = plannedWaypoints.map { loc ->
                    mapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude
                    )
                }

                val rideData = mapOf(
                    "user_id" to userId,
                    "start_time" to startTime,
                    "planned_waypoints" to waypointsData,
                    "deviation_threshold_m" to deviationThresholdM,
                    "status" to "active",
                    "created_at" to System.currentTimeMillis()
                )

                val response = supabaseClient
                    .from("rides")
                    .insert(rideData)
                    .execute()

                // Extract the ID from the response
                val data = response.data as List<<Map<String, Any>>>
                val rideId = data[0]["id"] as String
                rideId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to create ride: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error creating ride: ${e.message}", e)
            }
        }
    }

    /**
     * Updates the end time of a ride
     */
    suspend fun updateRideEndTime(
        rideId: String,
        endTime: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mapOf(
                    "end_time" to endTime,
                    "status" to "completed",
                    "updated_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from("rides")
                    .update(updates)
                    .eq("id", rideId)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update ride end time: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating ride end time: ${e.message}", e)
            }
        }
    }

    /**
     * Adds a waypoint to a ride's track
     */
    suspend fun addRideWaypoint(
        rideId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val waypointData = mapOf(
                    "ride_id" to rideId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "accuracy" to accuracy.toDouble(),
                    "timestamp" to timestamp
                )

                supabaseClient
                    .from("ride_waypoints")
                    .insert(waypointData)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to add ride waypoint: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error adding ride waypoint: ${e.message}", e)
            }
        }
    }

    /**
     * Updates the planned route for a ride
     */
    suspend fun updateRideRoute(
        rideId: String,
        plannedWaypoints: List<android.location.Location>,
        deviationThresholdM: Double
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Convert planned waypoints to storable format
                val waypointsData = plannedWaypoints.map { loc ->
                    mapOf(
                        "latitude" to loc.latitude,
                        "longitude" to loc.longitude
                    )
                }

                val updates = mapOf(
                    "planned_waypoints" to waypointsData,
                    "deviation_threshold_m" to deviationThresholdM,
                    "updated_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from("rides")
                    .update(updates)
                    .eq("id", rideId)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update ride route: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating ride route: ${e.message}", e)
            }
        }
    }

    /**
     * Gets ride information by ID
     */
    suspend fun getRideById(rideId: String): RideRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("rides")
                    .select("*")
                    .eq("id", rideId)
                    .single()
                    .execute()

                val data = response.data as Map<String, Any>
                return RideRecord(
                    id = data["id"] as String,
                    userId = data["user_id"] as String,
                    startTime = data["start_time"] as Long,
                    endTime = data["end_time"] as Long?,
                    status = data["status"] as String,
                    plannedWaypoints = parseWaypoints(data["planned_waypoints"] as List<*>),
                    deviationThresholdM = data["deviation_threshold_m"] as Double,
                    createdAt = data["created_at"] as Long,
                    updatedAt = data["updated_at"] as Long
                )
            } catch (e: PostgrestException) {
                if (e.code == "PGRST116") {
                    // No rows returned
                    return null
                }
                throw RuntimeException("Failed to get ride: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting ride: ${e.message}", e)
            }
        }
    }

    /**
     * Gets active rides for a user
     */
    suspend fun getActiveRidesForUser(userId: String): List<RideRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("rides")
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "active")
                    .order("start_time", ascending = false)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    RideRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        startTime = record["start_time"] as Long,
                        endTime = record["end_time"] as Long?,
                        status = record["status"] as String,
                        plannedWaypoints = parseWaypoints(record["planned_waypoints"] as List<*>),
                        deviationThresholdM = record["deviation_threshold_m"] as Double,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get active rides: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting active rides: ${e.message}", e)
            }
        }
    }

    /**
     * Gets ride history for a user
     */
    suspend fun getRideHistory(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<RideRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("rides")
                    .select("*")
                    .eq("user_id", userId)
                    .order("start_time", ascending = false)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    RideRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        startTime = record["start_time"] as Long,
                        endTime = record["end_time"] as Long?,
                        status = record["status"] as String,
                        plannedWaypoints = parseWaypoints(record["planned_waypoints"] as List<*>),
                        deviationThresholdM = record["deviation_threshold_m"] as Double,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get ride history: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting ride history: ${e.message}", e)
            }
        }
    }

    /**
     * Gets waypoints for a specific ride
     */
    suspend fun getRideWaypoints(
        rideId: String
    ): List<RideWaypoint> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("ride_waypoints")
                    .select("*")
                    .eq("ride_id", rideId)
                    .order("timestamp", ascending = true)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    RideWaypoint(
                        id = record["id"] as String,
                        rideId = record["ride_id"] as String,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        accuracy = record["accuracy"] as Float,
                        timestamp = record["timestamp"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get ride waypoints: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting ride waypoints: ${e.message}", e)
            }
        }
    }

    /**
     * Parses waypoint data from the database
     */
    private fun parseWaypoints(data: List<*>): List<android.location.Location> {
        val waypoints = ArrayList<android.location.Location>()
        for (item in data) {
            if (item is Map<*, *>) {
                val lat = item["latitude"] as? Double ?: 0.0
                val lon = item["longitude"] as? Double ?: 0.0
                val loc = android.location.Location("")
                loc.latitude = lat
                loc.longitude = lon
                waypoints.add(loc)
            }
        }
        return waypoints
    }
}

/**
 * Data class representing a ride record
 */
data class RideRecord(
    val id: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String,
    val plannedWaypoints: List<android.location.Location>,
    val deviationThresholdM: Double,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Data class representing a ride waypoint
 */
data class RideWaypoint(
    val id: String,
    val rideId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
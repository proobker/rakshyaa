package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import io.github.jmnarloch.supabase.kaft.PostgrestException
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext

/**
 * Repository for handling location data operations with Supabase
 */
@Singleton
class LocationRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    /**
     * Save a location update to the location_logs table
     */
    suspend fun saveLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val locationData = mapOf(
                    "user_id" to userId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "accuracy" to accuracy.toDouble(),
                    "timestamp" to timestamp,
                    "created_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from("location_logs")
                    .insert(locationData)
                    .execute()
            } catch (e: PostgrestException) {
                // Re-throw as runtime exception for the caller to handle
                throw RuntimeException("Failed to save location: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error saving location: ${e.message}", e)
            }
        }
    }

    /**
     * Get location history for a user within a time range
     */
    suspend fun getLocationHistory(
        userId: String,
        startTime: Long,
        endTime: Long = System.currentTimeMillis()
    ): List<LocationRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("location_logs")
                    .select("*")
                    .eq("user_id", userId)
                    .gte("timestamp", startTime)
                    .lte("timestamp", endTime)
                    .order("timestamp", ascending = false)
                    .execute()

                // Parse the response into LocationRecord objects
                val data = response.data as List<<Map<String, Any>>
                return data.map { record ->
                    LocationRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        accuracy = record["accuracy"] as Float,
                        timestamp = record["timestamp"] as Long,
                        createdAt = record["created_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get location history: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting location history: ${e.message}", e)
            }
        }
    }

    /**
     * Get the most recent location for a user
     */
    suspend fun getLastKnownLocation(userId: String): LocationRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("location_logs")
                    .select("*")
                    .eq("user_id", userId)
                    .order("timestamp", ascending = false)
                    .limit(1)
                    .execute()

                val data = response.data as List<<Map<String, Any>>
                if (data.isNotEmpty()) {
                    val record = data[0]
                    return LocationRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        accuracy = record["accuracy"] as Float,
                        timestamp = record["timestamp"] as Long,
                        createdAt = record["created_at"] as Long
                    )
                } else {
                    return null
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get last known location: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting last known location: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing a location record
 */
data class LocationRecord(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val createdAt: Long
)
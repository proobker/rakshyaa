package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import io.github.jmnarloch.supabase.kaft.PostgrestException
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.ArrayList
import java.util.List

/**
 * Repository for handling safe places data with Supabase and PostGIS
 */
@Singleton
class SafePlacesRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val SAFE_PLACES_TABLE = "safe_places"
        private const val USER_PLACES_TABLE = "user_safe_places"
    }

    /**
     * Gets nearby safe places using PostGIS geospatial queries
     *
     * @param latitude User's latitude
     * @param longitude User's longitude
     * @param radiusM Search radius in meters
     * @param placeTypes Types of places to search for (e.g., "hospital", "police", "fire_station")
     * @return List of nearby safe places sorted by distance
     */
    suspend fun getNearbySafePlaces(
        latitude: Double,
        longitude: Double,
        radiusM: Double,
        placeTypes: List<String>
    ): List<SafePlace> {
        return withContext(Dispatchers.IO) {
            try {
                // Using Supabase's RPC function for PostGIS distance query
                // We'll assume there's a function called 'get_nearby_safe_places'
                // that takes latitude, longitude, radius, and place types as parameters

                val params = mapOf(
                    "p_latitude" to latitude,
                    "p_longitude" to longitude,
                    "p_radius_m" to radiusM,
                    "p_place_types" to placeTypes
                )

                val response = supabaseClient
                    .rpc("get_nearby_safe_places", params)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    SafePlace(
                        id = record["id"] as String,
                        name = record["name"] as String,
                        placeType = record["place_type"] as String,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        description = record["description"] as String?,
                        address = record["address"] as String?,
                        distanceM = record["distance_m"] as Double
                    )
                }
            } catch (e: PostgrestException) {
                // Fallback to basic filtering if RPC function doesn't exist
                throw RuntimeException("Failed to get nearby safe places: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting nearby safe places: ${e.message}", e)
            }
        }
    }

    /**
     * Adds a user-submitted safe place
     */
    suspend fun addUserSubmittedPlace(
        userId: String,
        latitude: Double,
        longitude: Double,
        name: String,
        placeType: String,
        description: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val placeData = mapOf(
                    "user_id" to userId,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "name" to name,
                    "place_type" to placeType,
                    "description" to description,
                    "created_at" to System.currentTimeMillis()
                )

                val response = supabaseClient
                    .from(USER_PLACES_TABLE)
                    .insert(placeData)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                val placeId = data[0]["id"] as String
                placeId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to add user-submitted place: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error adding user-submitted place: ${e.message}", e)
            }
        }
    }

    /**
     * Gets user-submitted safe places
     */
    suspend fun getUserSubmittedPlaces(
        userId: String
    ): List<SafePlace> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(USER_PLACES_TABLE)
                    .select("*")
                    .eq("user_id", userId)
                    .order("created_at", ascending = false)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    SafePlace(
                        id = record["id"] as String,
                        name = record["name"] as String,
                        placeType = record["place_type"] as String,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        description = record["description"] as String?,
                        address = record["address"] as String?,
                        distanceM = 0.0 // Distance would need to be calculated separately
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get user-submitted places: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting user-submitted places: ${e.message}", e)
            }
        }
    }

    /**
     * Gets all safe places (system + user-submitted) near a location
     * This combines both tables for a comprehensive search
     */
    suspend fun getAllNearbyPlaces(
        latitude: Double,
        longitude: Double,
        radiusM: Double
    ): List<SafePlace> {
        return withContext(Dispatchers.IO) {
            try {
                // Get system safe places
                val systemPlaces = getNearbySafePlaces(
                    latitude = latitude,
                    longitude = longitude,
                    radiusM = radiusM,
                    placeTypes = listOf("hospital", "police", "fire_station")
                )

                // Get user-submitted places for this user (we'd need the user ID)
                #TODO: In a real implementation, we would get the current user ID
                #and fetch their submitted places, then calculate distances

                // For now, just return system places
                return systemPlaces
            } catch (e: Exception) {
                throw RuntimeException("Failed to get all nearby places: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing a safe place
 */
data class SafePlace(
    val id: String,
    val name: String,
    val placeType: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val address: String?,
    val distanceM: Double  // Distance from user in meters
)
package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import io.github.jmnarloch.supabase.kaft.PostgrestException
import io.github.jmnarloch.supabase.kaft.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext

/**
 * Repository for handling SOS incident data operations with Supabase
 */
@Singleton
class IncidentRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    /**
     * Create a new SOS incident record
     */
    suspend fun createIncident(incidentData: Map<String, Any>): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("incidents")
                    .insert(incidentData)
                    .execute()

                // Extract the ID from the response
                val data = response.data as List<<Map<String, Any>>>
                val incidentId = data[0]["id"] as String
                incidentId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to create incident: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error creating incident: ${e.message}", e)
            }
        }
    }

    /**
     * Update an existing incident record
     */
    suspend fun updateIncident(incidentId: String, updates: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .from("incidents")
                    .update(updates)
                    .eq("id", incidentId)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update incident: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating incident: ${e.message}", e)
            }
        }
    }

    /**
     * Get incident by ID
     */
    suspend fun getIncidentById(incidentId: String): IncidentRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("incidents")
                    .select("*")
                    .eq("id", incidentId)
                    .single()
                    .execute()

                val data = response.data as Map<String, Any>
                return IncidentRecord(
                    id = data["id"] as String,
                    userId = data["user_id"] as String,
                    isFalseAlarm = data["is_false_alarm"] as Boolean,
                    status = data["status"] as String,
                    activatedAt = data["activated_at"] as Long,
                    createdAt = data["created_at"] as Long,
                    updatedAt = data["updated_at"] as Long
                )
            } catch (e: PostgrestException) {
                if (e.code == "PGRST116") {
                    // No rows returned
                    return null
                }
                throw RuntimeException("Failed to get incident: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting incident: ${e.message}", e)
            }
        }
    }

    /**
     * Get active incidents for a user
     */
    suspend fun getActiveIncidentsForUser(userId: String): List<IncidentRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("incidents")
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "active")
                    .order("activated_at", ascending = false)
                    .execute()

                val data = response.data as List<<Map<String, Any>>
                return data.map { record ->
                    IncidentRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        isFalseAlarm = record["is_false_alarm"] as Boolean,
                        status = record["status"] as String,
                        activatedAt = record["activated_at"] as Long,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get active incidents: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting active incidents: ${e.message}", e)
            }
        }
    }

    /**
     * Get incident history for a user
     */
    suspend fun getIncidentHistory(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<IncidentRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("incidents")
                    .select("*")
                    .eq("user_id", userId)
                    .order("activated_at", ascending = false)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>
                return data.map { record ->
                    IncidentRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        isFalseAlarm = record["is_false_alarm"] as Boolean,
                        status = record["status"] as String,
                        activatedAt = record["activated_at"] as Long,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get incident history: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting incident history: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing an incident record
 */
data class IncidentRecord(
    val id: String,
    val userId: String,
    val isFalseAlarm: Boolean,
    val status: String,
    val activatedAt: Long,
    val createdAt: Long,
    val updatedAt: Long
)
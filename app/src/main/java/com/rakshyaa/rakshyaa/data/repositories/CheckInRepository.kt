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
 * Repository for handling check-in data with Supabase
 */
@Singleton
class CheckInRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val CHECK_INS_TABLE = "check_ins"
        private const val CHECK_IN_RESPONSES_TABLE = "check_in_responses"
    }

    /**
     * Schedules a new check-in
     */
    suspend fun scheduleCheckIn(
        userId: String,
        checkInId: String,
        scheduledTime: Long,
        gracePeriodMin: Int,
        latitude: Double,
        longitude: Double,
        radiusM: Double,
        isRecurring: Boolean = false,
        recurrencePattern: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val checkInData = mapOf(
                    "id" to checkInId,
                    "user_id" to userId,
                    "scheduled_time" to scheduledTime,
                    "grace_period_min" to gracePeriodMin,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "radius_m" to radiusM,
                    "is_recurring" to isRecurring,
                    "recurrence_pattern" to recurrencePattern,
                    "status" to "scheduled", // scheduled, completed, missed
                    "created_at" to System.currentTimeMillis()
                )

                val response = supabaseClient
                    .from(CHECK_INS_TABLE)
                    .insert(checkInData)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                val id = data[0]["id"] as String
                id
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to schedule check-in: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error scheduling check-in: ${e.message}", e)
            }
        }
    }

    /**
     * Completes a check-in (user confirmed safety)
     */
    suspend fun completeCheckIn(
        checkInId: String,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mapOf(
                    "status" to "completed",
                    "completed_at" to timestamp,
                    "response_latitude" to latitude,
                    "response_longitude" to longitude,
                    "updated_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from(CHECK_INS_TABLE)
                    .update(updates)
                    .eq("id", checkInId)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to complete check-in: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error completing check-in: ${e.message}", e)
            }
        }
    }

    /**
     * Marks a check-in as missed (user did not respond in time)
     */
    suspend fun markCheckInAsMissed(
        checkInId: String,
        timestamp: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mapOf(
                    "status" to "missed",
                    "missed_at" to timestamp,
                    "updated_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from(CHECK_INS_TABLE)
                    .update(updates)
                    .eq("id", checkInId)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to mark check-in as missed: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error marking check-in as missed: ${e.message}", e)
            }
        }
    }

    /**
     * Gets scheduled check-ins for a user
     */
    suspend fun getScheduledCheckIns(
        userId: String
    ): List<CheckInRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(CHECK_INS_TABLE)
                    .select("*")
                    .eq("user_id", userId)
                    .eq("status", "scheduled")
                    .order("scheduled_time", ascending = true)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    CheckInRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        scheduledTime = record["scheduled_time"] as Long,
                        gracePeriodMin = record["grace_period_min"] as Int,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        radiusM = record["radius_m"] as Double,
                        isRecurring = record["is_recurring"] as Boolean,
                        recurrencePattern = record["recurrence_pattern"] as String?,
                        status = record["status"] as String,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get scheduled check-ins: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting scheduled check-ins: ${e.message}", e)
            }
        }
    }

    /**
     * Gets check-in history for a user
     */
    suspend fun getCheckInHistory(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): List<CheckInRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(CHECK_INS_TABLE)
                    .select("*")
                    .eq("user_id", userId)
                    .order("scheduled_time", ascending = false)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    CheckInRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        scheduledTime = record["scheduled_time"] as Long,
                        gracePeriodMin = record["grace_period_min"] as Int,
                        latitude = record["latitude"] as Double,
                        longitude = record["longitude"] as Double,
                        radiusM = record["radius_m"] as Double,
                        isRecurring = record["is_recurring"] as Boolean,
                        recurrencePattern = record["recurrence_pattern"] as String?,
                        status = record["status"] as String,
                        completedAt = record["completed_at"] as Long?,
                        missedAt = record["missed_at"] as Long?,
                        responseLatitude = record["response_latitude"] as Double?,
                        responseLongitude = record["response_longitude"] as Double?,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get check-in history: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting check-in history: ${e.message}", e)
            }
        }
    }

    /**
     * Gets a specific check-in by ID
     */
    suspend fun getCheckInById(checkInId: String): CheckInRecord? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(CHECK_INS_TABLE)
                    .select("*")
                    .eq("id", checkInId)
                    .single()
                    .execute()

                val data = response.data as Map<String, Any>
                return CheckInRecord(
                    id = data["id"] as String,
                    userId = data["user_id"] as String,
                    scheduledTime = data["scheduled_time"] as Long,
                    gracePeriodMin = data["grace_period_min"] as Int,
                    latitude = data["latitude"] as Double,
                    longitude = data["longitude"] as Double,
                    radiusM = data["radius_m"] as Double,
                    isRecurring = data["is_recurring"] as Boolean,
                    recurrencePattern = data["recurrence_pattern"] as String?,
                    status = data["status"] as String,
                    completedAt = data["completed_at"] as Long?,
                    missedAt = data["missed_at"] as Long?,
                    responseLatitude = data["response_latitude"] as Double?,
                    responseLongitude = data["response_longitude"] as Double?,
                    createdAt = data["created_at"] as Long,
                    updatedAt = data["updated_at"] as Long
                )
            } catch (e: PostgrestException) {
                if (e.code == "PGRST116") {
                    // No rows returned
                    return null
                }
                throw RuntimeException("Failed to get check-in: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting check-in: ${e.message}", e)
            }
        }
    }

    /**
     * Records a check-in response (for detailed tracking)
     */
    suspend fun recordCheckInResponse(
        checkInId: String,
        responseType: String, // "check_in", "missed", "snoozed"
        latitude: Double?,
        longitude: Double?,
        timestamp: Long,
        notes: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val responseData = mapOf(
                    "check_in_id" to checkInId,
                    "response_type" to responseType,
                    "response_latitude" to latitude,
                    "response_longitude" to longitude,
                    "timestamp" to timestamp,
                    "notes" to notes,
                    "created_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from(CHECK_IN_RESPONSES_TABLE)
                    .insert(responseData)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to record check-in response: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error recording check-in response: ${e.message}", e)
            }
        }
    }

    /**
     * Gets check-in responses for a specific check-in
     */
    suspend fun getCheckInResponses(
        checkInId: String
    ): List<CheckInResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(CHECK_IN_RESPONSES_TABLE)
                    .select("*")
                    .eq("check_in_id", checkInId)
                    .order("timestamp", ascending = true)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    CheckInResponse(
                        id = record["id"] as String,
                        checkInId = record["check_in_id"] as String,
                        responseType = record["response_type"] as String,
                        responseLatitude = record["response_latitude"] as Double?,
                        responseLongitude = record["response_longitude"] as Double?,
                        timestamp = record["timestamp"] as Long,
                        notes = record["notes"] as String?,
                        createdAt = record["created_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get check-in responses: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting check-in responses: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing a check-in record
 */
data class CheckInRecord(
    val id: String,
    val userId: String,
    val scheduledTime: Long,
    val gracePeriodMin: Int,
    val latitude: Double,
    val longitude: Double,
    val radiusM: Double,
    val isRecurring: Boolean,
    val recurrencePattern: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val missedAt: Long? = null,
    val responseLatitude: Double? = null,
    val responseLongitude: Double? = null
)

/**
 * Data class representing a check-in response
 */
data class CheckInResponse(
    val id: String,
    val checkInId: String,
    val responseType: String,
    val responseLatitude: Double? = null,
    val responseLongitude: Double? = null,
    val timestamp: Long,
    val notes: String? = null,
    val createdAt: Long
)
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
 * Repository for handling emergency contacts data with Supabase
 * Note: Encryption/decryption is handled by the service layer
 */
@Singleton
class EmergencyContactsRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val EMERGENCY_CONTACTS_TABLE = "emergency_contacts"
    }

    /**
     * Adds an emergency contact
     *
     * @note The service layer should encrypt sensitive fields (phone_number, public_key) before calling this
     */
    suspend fun addEmergencyContact(
        userId: String,
        name: String,
        encryptedPhoneNumber: String,
        relationship: String,
        encryptedPublicKey: String,
        isPrimary: Boolean
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val contactData = mapOf(
                    "user_id" to userId,
                    "name" to name,
                    "phone_number" to encryptedPhoneNumber,
                    "relationship" to relationship,
                    "public_key" to encryptedPublicKey,
                    "is_primary" to isPrimary,
                    "created_at" to System.currentTimeMillis()
                )

                val response = supabaseClient
                    .from(EMERGENCY_CONTACTS_TABLE)
                    .insert(contactData)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                val contactId = data[0]["id"] as String
                contactId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to add emergency contact: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error adding emergency contact: ${e.message}", e)
            }
        }
    }

    /**
     * Updates an emergency contact
     *
     * @note The service layer should encrypt sensitive fields (phone_number, public_key) before calling this
     */
    suspend fun updateEmergencyContact(
        contactId: String,
        name: String,
        encryptedPhoneNumber: String,
        relationship: String,
        encryptedPublicKey: String,
        isPrimary: Boolean
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mapOf(
                    "name" to name,
                    "phone_number" to encryptedPhoneNumber,
                    "relationship" to relationship,
                    "public_key" to encryptedPublicKey,
                    "is_primary" to isPrimary,
                    "updated_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from(EMERGENCY_CONTACTS_TABLE)
                    .update(updates)
                    .eq("id", contactId)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update emergency contact: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating emergency contact: ${e.message}", e)
            }
        }
    }

    /**
     * Removes an emergency contact
     */
    suspend fun removeEmergencyContact(
        contactId: String,
        userId: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .from(EMERGENCY_CONTACTS_TABLE)
                    .delete()
                    .match(mapOf(
                        "id" to contactId,
                        "user_id" to userId
                    ))
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to remove emergency contact: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error removing emergency contact: ${e.message}", e)
            }
        }
    }

    /**
     * Gets emergency contacts for a user
     */
    suspend fun getEmergencyContacts(
        userId: String
    ): List<EmergencyContact> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(EMERGENCY_CONTACTS_TABLE)
                    .select("*")
                    .eq("user_id", userId)
                    .order("is_primary", descending = true) // Primary contacts first
                    .order("created_at", ascending = true)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    EmergencyContact(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        name = record["name"] as String,
                        phoneNumber = record["phone_number"] as String, // Encrypted - service will decrypt
                        relationship = record["relationship"] as String,
                        publicKey = record["public_key"] as String, // Encrypted - service will decrypt
                        isPrimary = record["is_primary"] as Boolean,
                        createdAt = record["created_at"] as Long,
                        updatedAt = record["updated_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get emergency contacts: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting emergency contacts: ${e.message}", e)
            }
        }
    }

    /**
     * Gets a specific emergency contact by ID
     */
    suspend fun getEmergencyContactById(
        contactId: String,
        userId: String
    ): EmergencyContact? {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(EMERGENCY_CONTACTS_TABLE)
                    .select("*")
                    .eq("id", contactId)
                    .eq("user_id", userId)
                    .single()
                    .execute()

                val data = response.data as Map<String, Any>
                return EmergencyContact(
                    id = data["id"] as String,
                    userId = data["user_id"] as String,
                    name = data["name"] as String,
                    phoneNumber = data["phone_number"] as String, // Encrypted - service will decrypt
                    relationship = data["relationship"] as String,
                    publicKey = data["publicKey"] as String, // Encrypted - service will decrypt
                    isPrimary = data["is_primary"] as Boolean,
                    createdAt = data["created_at"] as Long,
                    updatedAt = data["updated_at"] as Long
                )
            } catch (e: PostgrestException) {
                if (e.code == "PGRST116") {
                    // No rows returned
                    return null
                }
                throw RuntimeException("Failed to get emergency contact: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting emergency contact: ${e.message}", e)
            }
        }
    }

    /**
     * Records an escalation for a missed check-in
     */
    suspend fun escalateMissedCheckIn(
        userId: String,
        checkInId: String,
        timestamp: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val escalationData = mapOf(
                    "user_id" to userId,
                    "check_in_id" to checkInId,
                    "timestamp" to timestamp,
                    "created_at" to System.currentTimeMillis()
                )

                supabaseClient
                    .from("emergency_contact_escalations")
                    .insert(escalationData)
                    .execute()
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to record emergency contact escalation: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error recording emergency contact escalation: ${e.message}", e)
            }
        }
    }

    /**
     * Gets escalation history for emergency contacts
     */
    suspend fun getEscalationHistory(
        userId: String
    ): List<EscalationRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from("emergency_contact_escalations")
                    .select("*")
                    .eq("user_id", userId)
                    .order("timestamp", ascending = false)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    EscalationRecord(
                        id = record["id"] as String,
                        userId = record["user_id"] as String,
                        checkInId = record["check_in_id"] as String,
                        timestamp = record["timestamp"] as Long,
                        createdAt = record["created_at"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get escalation history: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting escalation history: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing an emergency contact
 * Note: phoneNumber and publicKey fields are encrypted and should be decrypted by the service layer
 */
data class EmergencyContact(
    val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String, // Encrypted - service layer will decrypt
    val relationship: String,
    val publicKey: String, // Encrypted - service layer will decrypt
    val isPrimary: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Data class representing an escalation record
 */
data class EscalationRecord(
    val id: String,
    val userId: String,
    val checkInId: String,
    val timestamp: Long,
    val createdAt: Long
)
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
 * Repository for handling legal help content with Supabase
 */
@Singleton
class LegalHelpRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val LEGAL_ARTICLES_TABLE = "legal_articles"
        private const val EMERGENCY_NUMBERS_TABLE = "emergency_numbers"
        private const val SUPPORT_RESOURCES_TABLE = "support_resources"
    }

    /**
     * Gets legal articles
     */
    suspend fun getLegalArticles(
        limit: Int = 100,
        offset: Int = 0
    ): List<LegalArticle> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(LEGAL_ARTICLES_TABLE)
                    .select("*")
                    .order("last_updated", ascending = false)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    LegalArticle(
                        id = record["id"] as String,
                        title = record["title"] as String,
                        content = record["content"] as String,
                        category = record["category"] as String?,
                        lastUpdated = record["last_updated"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get legal articles: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting legal articles: ${e.message}", e)
            }
        }
    }

    /**
     * Gets legal articles by category
     */
    suspend fun getLegalArticlesByCategory(
        category: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<LegalArticle> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(LEGAL_ARTICLES_TABLE)
                    .select("*")
                    .eq("category", category)
                    .order("last_updated", ascending = false)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    LegalArticle(
                        id = record["id"] as String,
                        title = record["title"] as String,
                        content = record["content"] as String,
                        category = record["category"] as String,
                        lastUpdated = record["last_updated"] as Long
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get legal articles by category: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting legal articles by category: ${e.message}", e)
            }
        }
    }

    /**
     * Gets emergency numbers
     */
    suspend fun getEmergencyNumbers(
        limit: Int = 100,
        offset: Int = 0
    ): List<EmergencyNumber> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(EMERGENCY_NUMBERS_TABLE)
                    .select("*")
                    .order("name", ascending = true)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    EmergencyNumber(
                        id = record["id"] as String,
                        name = record["name"] as String,
                        number = record["number"] as String,
                        description = record["description"] as String?,
                        isInternational = record["is_international"] as Boolean
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get emergency numbers: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting emergency numbers: ${e.message}", e)
            }
        }
    }

    /**
     * Gets support resources
     */
    suspend fun getSupportResources(
        limit: Int = 100,
        offset: Int = 0
    ): List<SupportResource> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(SUPPORT_RESOURCES_TABLE)
                    .select("*")
                    .order("name", ascending = true)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    SupportResource(
                        id = record["id"] as String,
                        name = record["name"] as String,
                        description = record["description"] as String?,
                        phoneNumber = record["phone_number"] as String?,
                        website = record["website"] as String?,
                        category = record["category"] as String?
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get support resources: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting support resources: ${e.message}", e)
            }
        }
    }

    /**
     * Gets support resources by category
     */
    suspend fun getSupportResourcesByCategory(
        category: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<SupportResource> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .from(SUPPORT_RESOURCES_TABLE)
                    .select("*")
                    .eq("category", category)
                    .order("name", ascending = true)
                    .limit(limit.toString())
                    .offset(offset.toString())
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                return data.map { record ->
                    SupportResource(
                        id = record["id"] as String,
                        name = record["name"] as String,
                        description = record["description"] as String?,
                        phoneNumber = record["phone_number"] as String?,
                        website = record["website"] as String?,
                        category = record["category"] as String
                    )
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to get support resources by category: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error getting support resources by category: ${e.message}", e)
            }
        }
    }

    /**
     * Adds a new legal article
     */
    suspend fun addLegalArticle(
        title: String,
        content: String,
        category: String?
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val articleData = mapOf(
                    "title" to title,
                    "content" to content,
                    "category" to category,
                    "last_updated" to System.currentTimeMillis()
                )

                val response = supabaseClient
                    .from(LEGAL_ARTICLES_TABLE)
                    .insert(articleData)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                val articleId = data[0]["id"] as String
                articleId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to add legal article: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error adding legal article: ${e.message}", e)
            }
        }
    }

    /**
     * Adds a new emergency number
     */
    suspend fun addEmergencyNumber(
        name: String,
        number: String,
        description: String?,
        isInternational: Boolean
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val numberData = mapOf(
                    "name" to name,
                    "number" to number,
                    "description" to description,
                    "is_international" to isInternational
                )

                val response = supabaseClient
                    .from(EMERGENCY_NUMBERS_TABLE)
                    .insert(numberData)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                val numberId = data[0]["id"] as String
                numberId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to add emergency number: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error adding emergency number: ${e.message}", e)
            }
        }
    }

    /**
     * Adds a new support resource
     */
    suspend fun addSupportResource(
        name: String,
        description: String?,
        phoneNumber: String?,
        website: String?,
        category: String?
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val resourceData = mapOf(
                    "name" to name,
                    "description" to description,
                    "phone_number" to phoneNumber,
                    "website" to website,
                    "category" to category
                )

                val response = supabaseClient
                    .from(SUPPORT_RESOURCES_TABLE)
                    .insert(resourceData)
                    .execute()

                val data = response.data as List<<Map<String, Any>>>
                val resourceId = data[0]["id"] as String
                resourceId
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to add support resource: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error adding support resource: ${e.message}", e)
            }
        }
    }

    /**
     * Updates a legal article
     */
    suspend fun updateLegalArticle(
        id: String,
        title: String?,
        content: String?,
        category: String?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any>()
                title?.let { updates["title"] = title }
                content?.let { updates["content"] = content }
                category?.let { updates["category"] = category }
                updates["last_updated"] = System.currentTimeMillis()

                if (updates.isNotEmpty()) {
                    supabaseClient
                        .from(LEGAL_ARTICLES_TABLE)
                        .update(updates)
                        .eq("id", id)
                        .execute()
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update legal article: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating legal article: ${e.message}", e)
            }
        }
    }

    /**
     * Updates an emergency number
     */
    suspend fun updateEmergencyNumber(
        id: String,
        name: String?,
        number: String?,
        description: String?,
        isInternational: Boolean?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any>()
                name?.let { updates["name"] = name }
                number?.let { updates["number"] = number }
                description?.let { updates["description"] = description }
                isInternational?.let { updates["is_international"] = isInternational }

                if (updates.isNotEmpty()) {
                    supabaseClient
                        .from(EMERGENCY_NUMBERS_TABLE)
                        .update(updates)
                        .eq("id", id)
                        .execute()
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update emergency number: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating emergency number: ${e.message}", e)
            }
        }
    }

    /**
     * Updates a support resource
     */
    suspend fun updateSupportResource(
        id: String,
        name: String?,
        description: String?,
        phoneNumber: String?,
        website: String?,
        category: String?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val updates = mutableMapOf<String, Any>()
                name?.let { updates["name"] = name }
                description?.let { updates["description"] = description }
                phoneNumber?.let { updates["phone_number"] = phoneNumber }
                website?.let { updates["website"] = website }
                category?.let { updates["category"] = category }

                if (updates.isNotEmpty()) {
                    supabaseClient
                        .from(SUPPORT_RESOURCES_TABLE)
                        .update(updates)
                        .eq("id", id)
                        .execute()
                }
            } catch (e: PostgrestException) {
                throw RuntimeException("Failed to update support resource: ${e.message}", e)
            } catch (e: Exception) {
                throw RuntimeException("Unexpected error updating support resource: ${e.message}", e)
            }
        }
    }
}

/**
 * Data class representing a legal article
 */
data class LegalArticle(
    val id: String,
    val title: String,
    val content: String,
    val category: String?,
    val lastUpdated: Long
)

/**
 * Data class representing an emergency number
 */
data class EmergencyNumber(
    val id: String,
    val name: String,
    val number: String,
    val description: String?,
    val isInternational: Boolean
)

/**
 * Data class representing a support resource
 */
data class SupportResource(
    val id: String,
    val name: String,
    val description: String?,
    val phoneNumber: String?,
    val website: String?,
    val category: String?
)
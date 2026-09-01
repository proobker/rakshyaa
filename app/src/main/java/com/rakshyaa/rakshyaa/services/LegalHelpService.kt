package com.rakshyaa.rakshyaa.services

import com.rakshyaa.rakshyaa.data.models.LegalResource
import com.rakshyaa.rakshyaa.data.repositories.LegalHelpRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalHelpService @Inject constructor(
    private val repo: LegalHelpRepository
) {

    suspend fun allResources(): List<LegalResource> = repo.getAll()

    suspend fun resourcesByCategory(category: String): List<LegalResource> =
        repo.byCategory(category)

    suspend fun addNote(title: String, body: String, phone: String? = null) {
        repo.addNote(title = title, body = body, phone = phone)
    }

    suspend fun search(query: String): List<LegalResource> {
        val lower = query.lowercase()
        return repo.getAll().filter {
            it.title.lowercase().contains(lower) || it.body.lowercase().contains(lower)
        }
    }
}
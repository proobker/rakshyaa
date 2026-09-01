package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.LegalResource
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides legal help resources. Data is bundled offline so it can be shown
 * with no network, and user-added notes are synced encrypted to the backend.
 */
@Singleton
class LegalHelpRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager
) : EncryptedListRepository<LegalResource>(
    store = store,
    sync = sync,
    key = "legal_resources",
    elementSerializer = LegalResource.serializer()
) {

    suspend fun getAll(): List<LegalResource> {
        val userNotes = loadAll()
        return DEFAULT_RESOURCES + userNotes
    }

    suspend fun byCategory(category: String): List<LegalResource> =
        getAll().filter { it.category == category }

    suspend fun addNote(title: String, body: String, phone: String? = null): LegalResource {
        val note = LegalResource(
            id = noteId(title, System.currentTimeMillis()),
            title = title,
            body = body,
            category = "user",
            phone = phone
        )
        modify { it + note }
        return note
    }

    private fun noteId(title: String, ts: Long): String =
        "note-${title.hashCode().toUInt().toString(16)}-$ts"

    companion object {
        private val DEFAULT_RESOURCES = listOf(
            LegalResource(
                "legal-1",
                "Right to Safety",
                "You have the right to live free from violence and harassment. "
                    + "If you feel unsafe, call the police and file a complaint.",
                "general",
                "100"
            ),
            LegalResource(
                "legal-2",
                "Women's Helpline",
                "The national women's helpline provides support, guidance and "
                    + "referral for women facing violence.",
                "helpline",
                "1091"
            ),
            LegalResource(
                "legal-3",
                "Police Emergency",
                "In an immediate emergency call the police. Keep your location "
                    + "sharing enabled so responders can find you.",
                "emergency",
                "100"
            ),
            LegalResource(
                "legal-4",
                "Domestic Violence Act",
                "Seek protection and legal aid if you are a victim of domestic "
                    + "violence. You may obtain a protection order from the court.",
                "general",
                "1091"
            )
        )
    }
}

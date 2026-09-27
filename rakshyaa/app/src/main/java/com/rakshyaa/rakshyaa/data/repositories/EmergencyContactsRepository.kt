package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedListRepository
import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.EmergencyContact
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores emergency contacts as an encrypted, cloud-synced datastore.
 */
@Singleton
class EmergencyContactsRepository @Inject constructor(
    store: EncryptedLocalStore,
    sync: SyncManager
) : EncryptedListRepository<EmergencyContact>(
    store = store,
    sync = sync,
    key = "emergency_contacts",
    elementSerializer = EmergencyContact.serializer()
) {

    suspend fun getAll(): List<EmergencyContact> = loadAll()

    suspend fun getPrimary(): EmergencyContact? =
        loadAll().firstOrNull { it.isPrimary }

    suspend fun add(
        name: String,
        phoneNumber: String,
        relationship: String,
        isPrimary: Boolean
    ): EmergencyContact {
        val contact = EmergencyContact(
            id = UUID.randomUUID().toString(),
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship,
            isPrimary = isPrimary,
            createdAt = System.currentTimeMillis()
        )
        if (isPrimary) {
            modify { list ->
                list.map { it.copy(isPrimary = false) } + contact
            }
        } else {
            modify { it + contact }
        }
        return contact
    }

    suspend fun update(
        id: String,
        name: String,
        phoneNumber: String,
        relationship: String,
        isPrimary: Boolean
    ) {
        modify { list ->
            var newPrimary = false
            val updated = list.map { c ->
                if (c.id == id) {
                    c.copy(
                        name = name,
                        phoneNumber = phoneNumber,
                        relationship = relationship,
                        isPrimary = isPrimary
                    ).also { if (isPrimary) newPrimary = true }
                } else {
                    if (isPrimary && c.isPrimary) c.copy(isPrimary = false) else c
                }
            }
            updated
        }
    }

    suspend fun remove(id: String) {
        modify { list -> list.filterNot { it.id == id } }
    }
}

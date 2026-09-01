package com.rakshyaa.rakshyaa.services

import com.rakshyaa.rakshyaa.data.models.EmergencyContact
import com.rakshyaa.rakshyaa.data.repositories.EmergencyContactsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactsService @Inject constructor(
    private val repo: EmergencyContactsRepository
) {

    suspend fun addContact(
        name: String,
        phoneNumber: String,
        relationship: String,
        isPrimary: Boolean = false
    ): EmergencyContact = repo.add(
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship,
        isPrimary = isPrimary
    )

    suspend fun listContacts(): List<EmergencyContact> = repo.getAll()

    suspend fun updateContact(
        id: String,
        name: String,
        phoneNumber: String,
        relationship: String,
        isPrimary: Boolean
    ) {
        repo.update(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship,
            isPrimary = isPrimary
        )
    }

    suspend fun removeContact(id: String) {
        repo.remove(id)
    }

    suspend fun primaryContact(): EmergencyContact? = repo.getPrimary()
}
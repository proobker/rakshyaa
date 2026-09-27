package com.rakshyaa.rakshyaa.services

import com.rakshyaa.rakshyaa.data.models.EmergencyContact
import com.rakshyaa.rakshyaa.data.repositories.EmergencyContactsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class EmergencyContactsServiceUnitTest {

    private val repo = mock(EmergencyContactsRepository::class.java)

    private val primary = EmergencyContact(
        id = "c1",
        name = "Mom",
        phoneNumber = "1234567890",
        relationship = "Mother",
        isPrimary = true
    )
    private val backup = EmergencyContact(
        id = "c2",
        name = "Sister",
        phoneNumber = "0987654321",
        relationship = "Sister",
        isPrimary = false
    )

    private val service = EmergencyContactsService(repo)

    @Test
    fun `listContacts delegates to repository`() = runTest {
        `when`(repo.getAll()).thenReturn(listOf(primary, backup))

        val result = service.listContacts()

        assertThat(result).containsExactly(primary, backup).inOrder()
        verify(repo).getAll()
    }

    @Test
    fun `addContact delegates to repository and returns created contact`() = runTest {
        `when`(repo.add("Mom", "1234567890", "Mother", true)).thenReturn(primary)

        val result = service.addContact("Mom", "1234567890", "Mother", isPrimary = true)

        assertThat(result).isEqualTo(primary)
        verify(repo).add("Mom", "1234567890", "Mother", true)
    }

    @Test
    fun `updateContact delegates to repository`() = runTest {
        service.updateContact("c1", "Mom", "1112223333", "Mother", true)

        verify(repo).update("c1", "Mom", "1112223333", "Mother", true)
    }

    @Test
    fun `removeContact delegates to repository`() = runTest {
        service.removeContact("c2")

        verify(repo).remove("c2")
    }

    @Test
    fun `primaryContact delegates to repository`() = runTest {
        `when`(repo.getPrimary()).thenReturn(primary)

        val result = service.primaryContact()

        assertThat(result).isEqualTo(primary)
        verify(repo).getPrimary()
    }

    @Test
    fun `primaryContact returns null when none is primary`() = runTest {
        `when`(repo.getPrimary()).thenReturn(null)

        assertThat(service.primaryContact()).isNull()
    }
}
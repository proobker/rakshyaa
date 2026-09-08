package com.rakshyaa.rakshyaa.services

import com.rakshyaa.rakshyaa.data.models.LegalResource
import com.rakshyaa.rakshyaa.data.repositories.LegalHelpRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class LegalHelpServiceUnitTest {

    private val repo = mock(LegalHelpRepository::class.java)

    private val sampleArticles = listOf(
        LegalResource(
            id = "1",
            title = "Rights of Women",
            body = "Know your legal rights in the workplace.",
            category = "rights"
        ),
        LegalResource(
            id = "2",
            title = "Filing an FIR",
            body = "Steps to file a first information report.",
            category = "procedure"
        )
    )

    private val service = LegalHelpService(repo)

    @Test
    fun `allResources delegates to repository`() = runTest {
        `when`(repo.getAll()).thenReturn(sampleArticles)

        val result = service.allResources()

        assertThat(result).containsExactlyElementsIn(sampleArticles).inOrder()
        verify(repo).getAll()
    }

    @Test
    fun `resourcesByCategory delegates to repository`() = runTest {
        val rights = listOf(sampleArticles[0])
        `when`(repo.byCategory("rights")).thenReturn(rights)

        val result = service.resourcesByCategory("rights")

        assertThat(result).containsExactly(sampleArticles[0])
        verify(repo).byCategory("rights")
    }

    @Test
    fun `search filters by title or body`() = runTest {
        `when`(repo.getAll()).thenReturn(sampleArticles)

        val byTitle = service.search("FIR")
        val byBody = service.search("workplace")
        val noMatch = service.search("xyz")

        assertThat(byTitle).containsExactly(sampleArticles[1])
        assertThat(byBody).containsExactly(sampleArticles[0])
        assertThat(noMatch).isEmpty()
    }

    @Test
    fun `addNote delegates to repository`() = runTest {
        service.addNote("Title", "Body", phone = "12345")

        verify(repo).addNote(title = "Title", body = "Body", phone = "12345")
    }

    @Test
    fun `deleteNote delegates to repository`() = runTest {
        service.deleteNote("abc")

        verify(repo).remove("abc")
    }

    @Test
    fun `search is case-insensitive`() = runTest {
        `when`(repo.getAll()).thenReturn(sampleArticles)

        val result = service.search("fir")

        assertThat(result).containsExactly(sampleArticles[1])
    }
}
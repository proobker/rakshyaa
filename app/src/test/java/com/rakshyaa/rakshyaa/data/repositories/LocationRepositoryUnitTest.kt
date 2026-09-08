package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class LocationRepositoryUnitTest {

    private val store = mock(EncryptedLocalStore::class.java)
    private val stored = mutableMapOf<String, String?>()
    private lateinit var repository: LocationRepository

    @Before
    fun setUp() {
        stored.clear()
        `when`(store.loadPlain(anyString())).thenAnswer { stored[it.getArgument(0)] }
        `when`(store.delete(anyString())).thenAnswer {
            stored.remove(it.getArgument(0)); true
        }
        doAnswer { inv ->
            stored[inv.getArgument<String>(0)] = inv.getArgument<String>(1)
            null
        }.`when`(store).savePlain(anyString(), anyString())
        repository = LocationRepository(store)
    }

    @Test
    fun `saveLocation persists and getLastKnownLocation returns the newest`() = runTest {
        repository.saveLocation(latitude = 27.7172, longitude = 85.3240, timestamp = 1000L)
        repository.saveLocation(latitude = 27.7180, longitude = 85.3250, timestamp = 2000L)

        val last = repository.getLastKnownLocation()

        assertThat(last).isNotNull()
        assertThat(last!!.latitude).isEqualTo(27.7180)
        assertThat(last.longitude).isEqualTo(85.3250)
        assertThat(last.timestamp).isEqualTo(2000L)
    }

    @Test
    fun `saveLocation records accuracy and flags`() = runTest {
        repository.saveLocation(27.7, 85.3, accuracy = 12.5f, timestamp = 1000L, isSos = true)

        val record = repository.getLastKnownLocation()

        assertThat(record!!.accuracy).isEqualTo(12.5f)
        assertThat(record.isSos).isTrue()
    }

    @Test
    fun `getLocationHistory returns most recent first`() = runTest {
        repository.saveLocation(latitude = 1.0, longitude = 1.0, timestamp = 100L)
        repository.saveLocation(latitude = 2.0, longitude = 2.0, timestamp = 200L)
        repository.saveLocation(latitude = 3.0, longitude = 3.0, timestamp = 300L)

        val history = repository.getLocationHistory()

        assertThat(history.map { it.timestamp }).containsExactly(300L, 200L, 100L).inOrder()
    }

    @Test
    fun `saveSosLocation creates a flagged record`() = runTest {
        repository.saveSosLocation(27.7, 85.3, accuracy = 5f)

        val record = repository.getLastKnownLocation()

        assertThat(record!!.isSos).isTrue()
        assertThat(record.latitude).isEqualTo(27.7)
    }

    @Test
    fun `clear removes all stored locations`() = runTest {
        repository.saveLocation(27.7, 85.3, timestamp = 1000L)

        repository.clear()

        assertThat(stored["location_logs"]).isNull()
        assertThat(repository.getLastKnownLocation()).isNull()
        verify(store).delete("location_logs")
    }

    @Test
    fun `returns empty history when nothing is stored`() {
        assertThat(repository.getLocationHistory()).isEmpty()
        assertThat(repository.getLastKnownLocation()).isNull()
    }
}
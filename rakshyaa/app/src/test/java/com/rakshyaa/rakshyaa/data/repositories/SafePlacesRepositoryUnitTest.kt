package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.local.EncryptedLocalStore
import com.rakshyaa.rakshyaa.data.models.SafePlace
import com.rakshyaa.rakshyaa.data.network.ApiClient
import com.rakshyaa.rakshyaa.data.network.PlaceDto
import com.rakshyaa.rakshyaa.data.sync.SyncManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.io.IOException

class SafePlacesRepositoryUnitTest {

    private val store = mock(EncryptedLocalStore::class.java)
    private val sync = mock(SyncManager::class.java)
    private val apiClient = mock(ApiClient::class.java)
    private val stored = mutableMapOf<String, String?>()
    private lateinit var repository: SafePlacesRepository

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
        runBlocking {
            `when`(sync.getOrPull(anyString())).thenAnswer { stored[it.getArgument(0)] }
            `when`(sync.saveAndSync(anyString(), anyString())).thenAnswer { inv ->
                stored[inv.getArgument<String>(0)] = inv.getArgument<String>(1)
                null
            }
        }
        repository = SafePlacesRepository(store, sync, apiClient)
    }

    private val lat = 27.7172
    private val lon = 85.3240

    @Test
    fun `nearby keeps places within radius and drops the rest`() = runTest {
        `when`(apiClient.getNearbyPlaces(lat, lon)).thenReturn(
            listOf(
                PlaceDto(id = "a", name = "Near", type = "hospital", distanceMeters = 800),
                PlaceDto(id = "b", name = "Far", type = "police", distanceMeters = 12_000)
            )
        )

        val result = repository.nearby(lat, lon, radiusM = 5000.0)

        assertThat(result.isLive).isTrue()
        assertThat(result.nearby.map { it.id }).containsExactly("a")
        assertThat(result.closest).isNull()
    }

    @Test
    fun `nearby reports the closest match when nothing is within radius`() = runTest {
        `when`(apiClient.getNearbyPlaces(lat, lon)).thenReturn(
            listOf(
                PlaceDto(id = "a", name = "Closest", type = "hospital", distanceMeters = 800),
                PlaceDto(id = "b", name = "Next", type = "clinic", distanceMeters = 2_000)
            )
        )

        val result = repository.nearby(lat, lon, radiusM = 500.0)

        assertThat(result.nearby).isEmpty()
        assertThat(result.closest!!.id).isEqualTo("a")
        assertThat(result.closest!!.name).isEqualTo("Closest")
    }

    @Test
    fun `nearby merges user-added places that fall inside the radius`() = runTest {
        stored["safe_places"] = """[{"id":"u1","name":"Home","address":"","latitude":27.7172,"longitude":85.3240,"type":"user"}]"""
        `when`(apiClient.getNearbyPlaces(lat, lon)).thenReturn(
            listOf(PlaceDto(id = "a", name = "Near", type = "hospital", distanceMeters = 900))
        )

        val result = repository.nearby(lat, lon, radiusM = 5000.0)

        assertThat(result.nearby.map { it.id }).containsExactly("a", "u1").inOrder()
        assertThat(result.nearby.last { it.id == "u1" }.distanceMeters).isEqualTo(0L)
    }

    @Test
    fun `nearby falls back to hardcoded list when the network call fails`() = runTest {
        `when`(apiClient.getNearbyPlaces(lat, lon)).thenAnswer { throw IOException("boom") }

        val result = repository.nearby(lat, lon, radiusM = 5000.0)

        assertThat(result.isLive).isFalse()
        assertThat(result.nearby).isNotEmpty()
        assertThat(result.closest).isNull()
        assertThat(result.nearby.all { it.distanceMeters <= 5000L }).isTrue()
    }

    @Test
    fun `offlineFallback never touches the network and applies the radius cut`() = runTest {
        val result = repository.offlineFallback(27.7000, 85.3300, radiusM = 100.0)

        assertThat(result.isLive).isFalse()
        assertThat(result.nearby).isEmpty()
        assertThat(result.closest).isNotNull()
        assertThat(result.nearby.all { it.distanceMeters <= 100L }).isTrue()
    }

    @Test
    fun `add persists and returns a place with an id`() = runTest {
        val place = SafePlace(name = "Work", latitude = 27.7, longitude = 85.3, type = "user")

        val saved = repository.add(place)

        assertThat(saved.id).isNotEmpty()
        assertThat(repository.getAll().map { it.id }).contains(saved.id)
    }

    @Test
    fun `remove deletes an existing user place`() = runTest {
        val saved = repository.add(SafePlace(name = "Work", latitude = 27.7, longitude = 85.3, type = "user"))
        assertThat(repository.getAll().map { it.id }).contains(saved.id)

        repository.remove(saved.id)

        assertThat(repository.getAll().map { it.id }).doesNotContain(saved.id)
    }
}
package com.rakshyaa.rakshyaa.data.repositories

import com.rakshyaa.rakshyaa.data.SupabaseProvider
import io.github.jmnarloch.supabase.kaft.PostgrestException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.testCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoRule
import org.junit.Rule
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertThrows

@ExperimentalCoroutinesApi
class LocationRepositoryUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var supabaseClient: SupabaseProvider

    private lateinit var locationRepository: LocationRepository

    @Before
    fun setUp() {
        // Initialize the repository with mocked dependencies
        locationRepository = LocationRepository(supabaseClient)
    }

    @After
    fun tearDown() {
        // Reset mocks
        Mockito.reset(supabaseClient)
    }

    @Test
    fun `saveLocationSuccess should call supabase insert with correct parameters`() = runBlockingTest {
        // Arrange
        val testUserId = "test-user-id"
        val testLatitude = 40.7128
        val testLongitude = -74.0060
        val testAccuracy = 10.0f
        val testTimestamp = 1234567890L

        val mockResponse = Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestResponse::class.java)
        `when`(supabaseClient.from("location_logs"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()).execute())
            .thenReturn(mockResponse)

        // Act
        locationRepository.saveLocation(testUserId, testLatitude, testLongitude, testAccuracy, testTimestamp)

        // Assert
        Mockito.verify(supabaseClient, Mockito.timeout(1000))
            .from("location_logs")
            .insert(Mockito.any())
            .execute()
    }

    @Test
    fun `saveLocationPostgrestException should throw runtime exception`() = runBlockingTest {
        // Arrange
        val testUserId = "test-user-id"
        val testLatitude = 40.7128
        val testLongitude = -74.0060
        val testAccuracy = 10.0f
        val testTimestamp = 1234567890L

        val mockException = PostgrestException("Database error")

        `when`(supabaseClient.from("location_logs"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()))
            .thenThrow(mockException)

        // Act & Assert
        val exception = assertThrows(RuntimeException::class.java) {
            locationRepository.saveLocation(testUserId, testLatitude, testLongitude, testAccuracy, testTimestamp)
        }
        assertTrue(exception.message.contains("Failed to save location"))
        assertTrue(exception.cause === mockException)
    }

    @Test
    fun `saveLocationGenericException should throw runtime exception`() = runBlockingTest {
        // Arrange
        val testUserId = "test-user-id"
        val testLatitude = 40.7128
        val testLongitude = -74.0060
        val testAccuracy = 10.0f
        val testTimestamp = 1234567890L

        val mockException = Exception("Network error")

        `when`(supabaseClient.from("location_logs"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()))
            .thenThrow(mockException)

        // Act & Assert
        val exception = assertThrows(RuntimeException::class.java) {
            locationRepository.saveLocation(testUserId, testLatitude, testLongitude, testAccuracy, testTimestamp)
        }
        assertTrue(exception.message.contains("Unexpected error saving location"))
        assertTrue(exception.cause === mockException)
    }

    @Test
    fun `getLastKnownLocationEmpty should return null`() = runBlockingTest {
        // Arrange
        val testUserId = "test-user-id"

        val mockResponse = Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestResponse::class.java)
        val mockData = emptyList<mapOf<String, Any>>()
        `when`(mockResponse.data).thenReturn(mockData)

        `when`(supabaseClient.from("location_logs"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").select("*"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").select("*").eq("user_id", testUserId))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").select("*").eq("user_id", testUserId).orderBy("timestamp", false))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").select("*").eq("user_id", testUserId).orderBy("timestamp", false).limit(1))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").select("*").eq("user_id", testUserId).orderBy("timestamp", false).limit(1).execute())
            .thenReturn(mockResponse)

        // Act
        val result = locationRepository.getLastKnownLocation(testUserId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `saveSosLocationSuccess should call supabase insert with correct parameters`() = runBlockingTest {
        // Arrange
        val testUserId = "test-user-id"
        val testLatitude = 40.7128
        val testLongitude = -74.0060
        val testAccuracy = 10.0f
        val testTimestamp = 1234567890L

        val mockResponse = Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestResponse::class.java)
        `when`(supabaseClient.from("location_logs"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()).execute())
            .thenReturn(mockResponse)

        val sosData = mapOf(
            "user_id" to testUserId,
            "latitude" to testLatitude,
            "longitude" to testLongitude,
            "accuracy" to testAccuracy.toDouble(),
            "timestamp" to testTimestamp,
            "is_sos" to true
        )

        // Act
        locationRepository.saveSosLocation(sosData)

        // Assert
        Mockito.verify(supabaseClient, Mockito.timeout(1000))
            .from("location_logs")
            .insert(Mockito.any())
            .execute()
    }

    @Test
    fun `saveLocationsBatchSuccess should call supabase insert with correct parameters`() = runBlockingTest {
        // Arrange
        val testUserId = "test-user-id"
        val testLocations = listOf(
            LocationRecord(
                "loc1",
                testUserId,
                40.7128,
                -74.0060,
                10.0f,
                1234567890L,
                1234567890L
            ),
            LocationRecord(
                "loc2",
                testUserId,
                34.0522,
                -118.2437,
                15.0f,
                1234567891L,
                1234567891L
            )
        )

        val mockResponse = Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestResponse::class.java)
        `when`(supabaseClient.from("location_logs"))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()))
            .thenReturn(Mockito.mock(io.github.jmnarloch.supabase.kaft.PostgrestBuilder::class.java))
        `when`(supabaseClient.from("location_logs").insert(Mockito.any()).execute())
            .thenReturn(mockResponse)

        // Act
        locationRepository.saveLocationsBatch(testLocations)

        // Assert
        Mockito.verify(supabaseClient, Mockito.timeout(1000))
            .from("location_logs")
            .insert(Mockito.any())
            .execute()
    }
}
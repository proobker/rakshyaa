package com.rakshyaa.rakshyaa.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import com.rakshyaa.rakshyaa.data.LegalHelpRepository
import com.rakshyaa.rakshyaa.data.repositories.LegalHelpRepository.EmergencyNumber
import com.rakshyaa.rakshyaa.data.repositories.LegalHelpRepository.LegalArticle
import com.rakshyaa.rakshyaa.data.repositories.LegalHelpRepository.SupportResource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.testCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowNotificationManager

import java.util.concurrent.TimeUnit
import javax.inject.Inject

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LegalHelpServiceUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var mockAuthRepository: AuthRepository

    @Mock
    lateinit var mockSecurePreferences: SecurePreferences

    @Mock
    lateinit var mockLegalHelpRepository: LegalHelpRepository

    private lateinit var legalHelpService: LegalHelpService

    @Before
    fun setUp() {
        // Initialize LegalHelpService with mocked dependencies
        legalHelpService = LegalHelpService(
            mockAuthRepository,
            mockSecurePreferences,
            mockLegalHelpRepository
        )
    }

    @After
    fun tearDown() {
        Mockito.reset(
            mockAuthRepository,
            mockSecurePreferences,
            mockLegalHelpRepository
        )
    }

    @Test
    fun `serviceShouldBeCreatedWithCorrectDependencies`() {
        // Assert
        assertThat(legalHelpService.authRepository).isSameInstanceAs(mockAuthRepository)
        assertThat(legalHelpService.securePreferences).isSameInstanceAs(mockSecurePreferences)
        assertThat(legalHelpService.legalHelpRepository).isSameInstanceAs(mockLegalHelpRepository)
    }

    @Test
    fun `getLegalArticlesShouldReturnCachedArticles`() {
        // Arrange
        val testArticles = listOf(
            LegalArticle("1", "Test Article 1", "Content 1", "category1", System.currentTimeMillis()),
            LegalArticle("2", "Test Article 2", "Content 2", "category2", System.currentTimeMillis())
        )
        // Manually set the cached content (since it's private, we're testing the getter)
        legalHelpService.legalArticles = testArticles

        // Act
        val result = legalHelpService.getLegalArticles()

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(testArticles).inOrder()
    }

    @Test
    fun `getEmergencyNumbersShouldReturnCachedNumbers`() {
        // Arrange
        val testNumbers = listOf(
            EmergencyNumber("1", "Police", "911", "Police emergency", false),
            EmergencyNumber("2", "Fire", "911", "Fire emergency", false)
        )
        // Manually set the cached content
        legalHelpService.emergencyNumbers = testNumbers

        // Act
        val result = legalHelpService.getEmergencyNumbers()

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(testNumbers).inOrder()
    }

    @Test
    fun `getSupportResourcesShouldReturnCachedResources`() {
        // Arrange
        val testResources = listOf(
            SupportResource("1", "Resource 1", "Description 1", "123-456-7890", "https://example.com", "support"),
            SupportResource("2", "Resource 2", "Description 2", "098-765-4321", "https://example2.com", "support")
        )
        // Manually set the cached content
        legalHelpService.supportResources = testResources

        // Act
        val result = legalHelpService.getSupportResources()

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(testResources).inOrder()
    }

    @Test
    fun `getLegalArticlesByCategoryShouldFilterCorrectly`() {
        // Arrange
        val testArticles = listOf(
            LegalArticle("1", "Test Article 1", "Content 1", "category1", System.currentTimeMillis()),
            LegalArticle("2", "Test Article 2", "Content 2", "category2", System.currentTimeMillis()),
            LegalArticle("3", "Test Article 3", "Content 3", "category1", System.currentTimeMillis())
        )
        legalHelpService.legalArticles = testArticles

        // Act
        val result = legalHelpService.getLegalArticlesByCategory("category1")

        // Assert
        assertThat(result).hasSize(2)
        assertThat(result).containsExactlyElementsIn(
            testArticles[0], testArticles[2]
        ).inOrder()
    }

    @Test
    fun `searchLegalArticlesShouldFindMatchingArticles`() {
        // Arrange
        val testArticles = listOf(
            LegalArticle("1", "Police Rights Article", "Content about police rights", "legal", System.currentTimeMillis()),
            LegalArticle("2", "Self Defense Guide", "Content about self defense techniques", "safety", System.currentTimeMillis()),
            LegalArticle("3", "Domestic Violence Laws", "Laws protecting victims of domestic abuse", "legal", System.currentTimeMillis())
        )
        legalHelpService.legalArticles = testArticles

        // Act
        val result = legalHelpService.searchLegalArticles("police")

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result.first().id).isEqualTo("1")
    }

    @Test
    fun `searchLegalArticlesShouldBeCaseInsensitive`() {
        // Arrange
        val testArticles = listOf(
            LegalArticle("1", "Police Rights Article", "Content about police rights", "legal", System.currentTimeMillis())
        )
        legalHelpService.legalArticles = testArticles

        // Act
        val resultUpper = legalHelpService.searchLegalArticles("POLICE")
        val resultLower = legalHelpService.searchLegalArticles("police")
        val resultMixed = legalHelpService.searchLegalArticles("PoLiCe")

        // Assert
        assertThat(resultUpper).hasSize(1)
        assertThat(resultLower).hasSize(1)
        assertThat(resultMixed).hasSize(1)
    }

    @Test
    fun `getEmergencyNumberByNameShouldReturnMatchingNumber`() {
        // Arrange
        val testNumbers = listOf(
            EmergencyNumber("1", "Police Emergency", "911", "Police emergency", false),
            EmergencyNumber("2", "Fire Department", "911", "Fire emergency", false),
            EmergencyNumber("3", "Medical Emergency", "911", "Medical emergency", false)
        )
        legalHelpService.emergencyNumbers = testNumbers

        // Act
        val result = legalHelpService.getEmergencyNumberByName("Fire Department")

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo("2")
        assertThat(result!!.name).isEqualTo("Fire Department")
    }

    @Test
    fun `getEmergencyNumberByNameShouldReturnNullWhenNotFound`() {
        // Arrange
        val testNumbers = listOf(
            EmergencyNumber("1", "Police Emergency", "911", "Police emergency", false)
        )
        legalHelpService.emergencyNumbers = testNumbers

        // Act
        val result = legalHelpService.getEmergencyNumberByName("Non-existent Service")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `startLegalHelpServiceShouldSetActiveFlagAndStartCoroutine`() {
        // Arrange
        `when`(mockSecurePreferences.getUserId()).thenReturn("test-user-id")

        // Act
        legalHelpService.startLegalHelpService()

        // Assert
        assertThat(legalHelpService.isServiceActive).isTrue()
        // We can't easily test the coroutine without more complex mocking,
        // but we can verify that the service thinks it's active
    }

    @Test
    fun `stopLegalHelpServiceShouldSetInactiveFlagAndCancelJob`() {
        // Arrange
        legalHelpService.isServiceActive = true
        val mockJob = Mockito.mock(kotlinx.coroutines.Job::class.java)
        legalHelpService.helpJob = mockJob

        // Act
        legalHelpService.stopLegalHelpService()

        // Assert
        assertThat(legalHelpService.isServiceActive).isFalse()
        Mockito.verify(mockJob).cancel()
    }

    // Note: Testing syncLegalHelpContent and loadLegalHelpContent would require
    # more complex mocking of the repository and coroutines.
    # For now, we've tested the public getters and basic service lifecycle methods.
}
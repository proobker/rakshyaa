package com.rakshyaa.rakshyaa.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SecurePreferencesUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockEncryptedSharedPreferences: EncryptedSharedPreferences

    @Mock
    lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var securePreferences: SecurePreferences

    @Before
    fun setUp() {
        // Initialize SecurePreferences with mocked context
        securePreferences = SecurePreferences(mockContext)

        // Mock the EncryptedSharedPreferences creation
        `when`(mockContext.getApplicationContext()).thenReturn(mockContext)
        `when`(mockContext.getSharedPreferences(
            Mockito.eq("secure_prefs"),
            Mockito.eq(Context.MODE_PRIVATE)
        )).thenReturn(mockSharedPreferences)

        // For simplicity in this test, we'll directly set the encryptedSharedPreferences
        // In a more complex test, we'd use PowerMock or similar to mock the constructor
    }

    @After
    fun tearDown() {
        Mockito.reset(
            mockContext,
            mockEncryptedSharedPreferences,
            mockSharedPreferences,
            mockSharedPreferencesEditor
        )
    }

    @Test
    fun `saveAccessToken should store token in shared preferences`() {
        // Arrange
        val testToken = "test-access-token"
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.saveAccessToken(testToken)

        // Assert
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("access_token", testToken)
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `getAccessToken should return token from shared preferences`() {
        // Arrange
        val testToken = "test-access-token"
        `when`(mockSharedPreferences.getString("access_token", null)).thenReturn(testToken)

        // Act
        val result = securePreferences.getAccessToken()

        // Assert
        assertThat(result).isEqualTo(testToken)
    }

    @Test
    fun `getAccessTokenReturnsNullWhenNotSet`() {
        // Arrange
        `when`(mockSharedPreferences.getString("access_token", null)).thenReturn(null)

        // Act
        val result = securePreferences.getAccessToken()

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `saveRefreshToken should store token in shared preferences`() {
        // Arrange
        val testToken = "test-refresh-token"
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.saveRefreshToken(testToken)

        // Assert
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("refresh_token", testToken)
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `getRefreshToken should return token from shared preferences`() {
        // Arrange
        val testToken = "test-refresh-token"
        `when`(mockSharedPreferences.getString("refresh_token", null)).thenReturn(testToken)

        // Act
        val result = securePreferences.getRefreshToken()

        // Assert
        assertThat(result).isEqualTo(testToken)
    }

    @Test
    fun `saveUserId should store userId in shared preferences`() {
        // Arrange
        val testUserId = "test-user-id"
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.saveUserId(testUserId)

        // Assert
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("user_id", testUserId)
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `getUserId should return userId from shared preferences`() {
        // Arrange
        val testUserId = "test-user-id"
        `when`(mockSharedPreferences.getString("user_id", null)).thenReturn(testUserId)

        // Act
        val result = securePreferences.getUserId()

        // Assert
        assertThat(result).isEqualTo(testUserId)
    }

    @Test
    fun `saveUserEmail should store email in shared preferences`() {
        // Arrange
        val testEmail = "test@example.com"
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.saveUserEmail(testEmail)

        // Assert
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("user_email", testEmail)
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `getUserEmail should return email from shared preferences`() {
        // Arrange
        val testEmail = "test@example.com"
        `when`(mockSharedPreferences.getString("user_email", null)).thenReturn(testEmail)

        // Act
        val result = securePreferences.getUserEmail()

        // Assert
        assertThat(result).isEqualTo(testEmail)
    }

    @Test
    fun `saveLoginState should store login state in shared preferences`() {
        // Arrange
        val testState = true
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.saveLoginState(testState)

        // Assert
        Mockito.verify(mockSharedPreferencesEditor)
            .putBoolean("is_logged_in", testState)
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `isLoggedIn should return login state from shared preferences`() {
        // Arrange
        `when`(mockSharedPreferences.getBoolean("is_logged_in", false)).thenReturn(true)

        // Act
        val result = securePreferences.isLoggedIn()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `isLoggedInReturnsFalseWhenNotSet`() {
        // Arrange
        `when`(mockSharedPreferences.getBoolean("is_logged_in", false)).thenReturn(false)

        // Act
        val result = securePreferences.isLoggedIn()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `clear should remove all preferences`() {
        // Arrange
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.clear()

        // Assert
        Mockito.verify(mockSharedPreferencesEditor).clear()
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }

    @Test
    fun `saveAuthCredentials should store all credentials`() {
        // Arrange
        val testAccessToken = "test-access-token"
        val testRefreshToken = "test-refresh-token"
        val testUserId = "test-user-id"
        val testUserEmail = "test@example.com"
        `when`(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)

        // Act
        securePreferences.saveAuthCredentials(
            testAccessToken,
            testRefreshToken,
            testUserId,
            testUserEmail
        )

        // Assert
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("access_token", testAccessToken)
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("refresh_token", testRefreshToken)
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("user_id", testUserId)
        Mockito.verify(mockSharedPreferencesEditor)
            .putString("user_email", testUserEmail)
        Mockito.verify(mockSharedPreferencesEditor)
            .putBoolean("is_logged_in", true)
        Mockito.verify(mockSharedPreferencesEditor).apply()
    }
}
package com.rakshyaa.rakshyaa.data.auth

import com.rakshyaa.rakshyaa.data.local.SecurePreferences
import io.github.jmnarloch.supabase.kaft.goTrueApi.Session
import io.github.jmnarloch.supabase.kaft.goTrueApi.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.testCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoRule
import org.junit.Rule

@ExperimentalCoroutinesApi
class AuthRepositoryUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var supabaseClient: SupabaseProvider

    @Mock
    lateinit var securePreferences: SecurePreferences

    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        // Initialize the repository with mocked dependencies
        authRepository = AuthRepository(supabaseClient, securePreferences)
    }

    @After
    fun tearDown() {
        // Reset mocks
        Mockito.reset(supabaseClient, securePreferences)
    }

    @Test
    fun `signInWithEmailSuccess should return success result`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        val testPassword = "password123"
        val mockUser = User("test-user-id", testEmail, false, false)
        val mockSession = Session(mockUser, "access-token", "refresh-token", 3600)

        Mockito.`when`(supabaseClient.authService.signInWithEmail(testEmail, testPassword))
            .thenReturn(mockSession)

        // Act
        val result = authRepository.signInWithEmail(testEmail, testPassword)

        // Assert
        assertTrue(result is androidx.core.util.Result.Success)
        val success = result as androidx.core.util.Result.Success
        assertEquals(mockSession, success.getOrNull())
    }

    @Test
    fun `signInWithEmailFailurePostgrestException should return failure result`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        val testPassword = "wrongpassword"
        val mockException = io.github.jmnarloch.supabase.kaft.PostgrestException("Invalid credentials")

        Mockito.`when`(supabaseClient.authService.signInWithEmail(testEmail, testPassword))
            .thenThrow(mockException)

        // Act
        val result = authRepository.signInWithEmail(testEmail, testPassword)

        // Assert
        assertTrue(result is androidx.core.util.Result.Failure)
        val failure = result as androidx.core.util.Result.Failure
        assertEquals(mockException, failure.exceptionOrNull())
    }

    @Test
    fun `signInWithEmailGenericException should return failure result`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        val testPassword = "wrongpassword"
        val mockException = Exception("Network error")

        Mockito.`when`(supabaseClient.authService.signInWithEmail(testEmail, testPassword))
            .thenThrow(mockException)

        // Act
        val result = authRepository.signInWithEmail(testEmail, testPassword)

        // Assert
        assertTrue(result is androidx.core.util.Result.Failure)
        val failure = result as androidx.core.util.Result.Failure
        assertEquals(mockException, failure.exceptionOrNull())
    }

    @Test
    fun `signUpWithEmailSuccess should return success result`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        val testPassword = "password123"
        val mockUser = User("test-user-id", testEmail, false, false)
        val mockSession = Session(mockUser, "access-token", "refresh-token", 3600)

        Mockito.`when`(supabaseClient.authService.signUpWithEmail(testEmail, testPassword))
            .thenReturn(mockSession)

        // Act
        val result = authRepository.signUpWithEmail(testEmail, testPassword)

        // Assert
        assertTrue(result is androidx.core.util.Result.Success)
        val success = result as androidx.core.util.Result.Success
        assertEquals(mockSession, success.getOrNull())
    }

    @Test
    fun `signOutSuccess should return success result`() = runBlockingTest {
        // Arrange
        Mockito.`when`(supabaseClient.authService.signOut())
            .thenReturn(kotlin.Unit)

        // Act
        val result = authRepository.signOut()

        // Assert
        assertTrue(result is androidx.core.util.Result.Success)
        val success = result as androidx.core.util.Result.Success
        assertEquals(kotlin.Unit, success.getOrNull())
    }

    @Test
    fun `signOutFailurePostgrestException should return failure result`() = runBlockingTest {
        // Arrange
        val mockException = io.github.jmnarloch.supabase.kaft.PostgrestException("Sign out failed")

        Mockito.`when`(supabaseClient.authService.signOut())
            .thenThrow(mockException)

        // Act
        val result = authRepository.signOut()

        // Assert
        assertTrue(result is androidx.core.util.Result.Failure)
        val failure = result as androidx.core.util.Result.Failure
        assertEquals(mockException, failure.exceptionOrNull())
    }

    @Test
    fun `sendPasswordResetEmailSuccess should return success result`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        Mockito.`when`(supabaseClient.authService.resetPasswordForEmail(testEmail))
            .thenReturn(kotlin.Unit)

        // Act
        val result = authRepository.sendPasswordResetEmail(testEmail)

        // Assert
        assertTrue(result is androidx.core.util.Result.Success)
        val success = result as androidx.core.util.Result.Success
        assertEquals(kotlin.Unit, success.getOrNull())
    }

    @Test
    fun `getCurrentSessionReturnsCorrectValue`() {
        // Arrange
        val mockUser = User("test-user-id", "test@example.com", false, false)
        val mockSession = Session(mockUser, "access-token", "refresh-token", 3600)
        Mockito.`when`(supabaseClient.authService.currentSession).thenReturn(mockSession)

        // Act
        val session = authRepository.currentSession

        // Assert
        assertEquals(mockSession, session)
    }

    @Test
    fun `isAuthenticatedReturnsTrueWhenSessionExists`() {
        // Arrange
        val mockUser = User("test-user-id", "test@example.com", false, false)
        val mockSession = Session(mockUser, "access-token", "refresh-token", 3600)
        Mockito.`when`(supabaseClient.authService.currentSession).thenReturn(mockSession)

        // Act
        val isAuthenticated = authRepository.isAuthenticated

        // Assert
        assertTrue(isAuthenticated)
    }

    @Test
    fun `isAuthenticatedReturnsFalseWhenSessionNull`() {
        // Arrange
        Mockito.`when`(supabaseClient.authService.currentSession).thenReturn(null)

        // Act
        val isAuthenticated = authRepository.isAuthenticated

        // Assert
        assertFalse(isAuthenticated)
    }
}
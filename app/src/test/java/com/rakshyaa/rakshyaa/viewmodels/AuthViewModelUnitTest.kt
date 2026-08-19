package com.rakshyaa.rakshyaa.viewmodels

import androidx.lifecycle.ViewModel
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel.UiState
import io.github.jmnarloch.supabase.kaft.goTrueApi.Session
import io.github.jmnarloch.supabase.kaft.goTrueApi.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectFirst
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
class AuthViewModelUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var authRepository: AuthRepository

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        // Initialize the view model with mocked dependencies
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        // Reset mocks
        Mockito.reset(authRepository)
    }

    @Test
    fun `updateEmail should update email in UI state`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"

        // Act
        viewModel.updateEmail(testEmail)

        // Assert
        val uiState = viewModel.uiState.collectFirst()
        assertEquals(testEmail, uiState.email)
    }

    @Test
    fun `updatePassword should update password in UI state`() = runBlockingTest {
        // Arrange
        val testPassword = "password123"

        // Act
        viewModel.updatePassword(testPassword)

        // Assert
        val uiState = viewModel.uiState.collectFirst()
        assertEquals(testPassword, uiState.password)
    }

    @Test
    fun `clearForm should clear email, password and error message`() = runBlockingTest {
        // Arrange
        viewModel.updateEmail("test@example.com")
        viewModel.updatePassword("password123")
        // Simulate an error
        viewModel.uiState.update { it.copy(errorMessage = "Some error") }

        // Act
        viewModel.clearForm()

        // Assert
        val uiState = viewModel.uiState.collectFirst()
        assertTrue(uiState.email.isEmpty())
        assertTrue(uiState.password.isEmpty())
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `signInWithEmail launches coroutine and calls repository`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        val testPassword = "password123"
        val mockSession = Session(
            User("test-user-id", testEmail, false, false),
            "access-token",
            "refresh-token",
            3600
        )

        Mockito.`when`(authRepository.signInWithEmail(testEmail, testPassword))
            .thenReturn androidx.core.util.Result.Success(mockSession)

        // Act
        viewModel.signInWithEmail(testEmail, testPassword)

        // Assert
        Mockito.verify(authRepository, Mockito.timeout(1000))
            .signInWithEmail(testEmail, testPassword)
    }

    @Test
    fun `signUpWithEmail launches coroutine and calls repository`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        val testPassword = "password123"
        val mockSession = Session(
            User("test-user-id", testEmail, false, false),
            "access-token",
            "refresh-token",
            3600
        )

        Mockito.`when`(authRepository.signUpWithEmail(testEmail, testPassword))
            .thenReturn androidx.core.util.Result.Success(mockSession)

        // Act
        viewModel.signUpWithEmail(testEmail, testPassword)

        // Assert
        Mockito.verify(authRepository, Mockito.timeout(1000))
            .signUpWithEmail(testEmail, testPassword)
    }

    @Test
    fun `signOut launches coroutine and calls repository`() = runBlockingTest {
        // Arrange
        Mockito.`when`(authRepository.signOut())
            .thenReturn androidx.core.util.Result.Success(kotlin.Unit)

        // Act
        viewModel.signOut()

        // Assert
        Mockito.verify(authRepository, Mockito.timeout(1000))
            .signOut()
    }

    @Test
    fun `sendPasswordResetEmail launches coroutine and calls repository`() = runBlockingTest {
        // Arrange
        val testEmail = "test@example.com"
        Mockito.`when`(authRepository.sendPasswordResetEmail(testEmail))
            .thenReturn androidx.core.util.Result.Success(kotlin.Unit)

        // Act
        viewModel.sendPasswordResetEmail(testEmail)

        // Assert
        Mockito.verify(authRepository, Mockito.timeout(1000))
            .sendPasswordResetEmail(testEmail)
    }
}
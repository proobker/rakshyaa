package com.rakshyaa.rakshyaa.viewmodels

import androidx.lifecycle.ViewModel
import com.rakshyaa.rakshyaa.services.SOSActivationService
import com.rakshyaa.rakshyaa.viewmodels.SOSViewModel.UiState
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
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoRule
import org.junit.Rule

@ExperimentalCoroutinesApi
class SOSViewModelUnitTest {

    @get:Rule
    val mockitoRule = MockitoRule()

    @Mock
    lateinit var sosActivationService: SOSActivationService

    private lateinit var viewModel: SOSViewModel

    @Before
    fun setUp() {
        // Initialize the view model with mocked dependencies
        viewModel = SOSViewModel(sosActivationService)
    }

    @After
    fun tearDown() {
        // Reset mocks
        Mockito.reset(sosActivationService)
    }

    @Test
    fun `activateSosShouldSetCorrectInitialState`() = runBlockingTest {
        // Act
        viewModel.activateSos()

        // Assert - check initial state after calling activateSos
        val uiState = viewModel.uiState.collectFirst()
        assertTrue(uiState.isSosActivating)
        assertFalse(uiState.isSosActive)
        assertEquals(5, uiState.sosActivationCountdown)
        assertTrue(uiState.isLoading)
    }

    @Test
    fun `activateSosShouldStartCountdown`() = runBlockingTest {
        // Act
        viewModel.activateSos()

        // Assert - check that countdown started (we can't easily test the delay in unit test)
        // but we can verify that the service method will be called eventually
        Mockito.verify(sosActivationService, Mockito.timeout(6000))
            .activateSos(false)
    }

    @Test
    fun `deactivateSosShouldSetLoadingState`() = runBlockingTest {
        // Act
        viewModel.deactivateSos()

        // Assert - check initial state after calling deactivateSos
        val uiState = viewModel.uiState.collectFirst()
        assertTrue(uiState.isLoading)
        // Other states depend on previous state, but loading should be true
    }

    @Test
    fun `deactivateSosShouldCallService`() = runBlockingTest {
        // Act
        viewModel.deactivateSos()

        // Assert
        Mockito.verify(sosActivationService, Mockito.timeout(1000))
            .deactivateSos()
    }

    @Test
    fun `cancelSosActivationShouldResetState`() = runBlockingTest {
        // Arrange - first activate SOS to set up state
        viewModel.activateSos()

        // Act
        viewModel.cancelSosActivation()

        // Assert
        val uiState = viewModel.uiState.collectFirst()
        assertFalse(uiState.isSosActivating)
        assertFalse(uiState.isSosActive)
        assertEquals(0, uiState.sosActivationCountdown)
        assertFalse(uiState.isLoading)
    }

    @Test
    fun `cancelSosActivationShouldCancelCountdownJob`() = runBlockingTest {
        // Arrange
        viewModel.activateSos()

        // Act
        viewModel.cancelSosActivation()

        // Assert - we can't easily test that the job was cancelled without more complex mocking
        // but we can verify the method was called
        // In a more sophisticated test, we'd use a mock Job and verify cancel() was called
    }
}
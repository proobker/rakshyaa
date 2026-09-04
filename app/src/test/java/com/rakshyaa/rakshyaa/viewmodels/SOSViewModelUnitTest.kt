package com.rakshyaa.rakshyaa.viewmodels

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SOSViewModelUnitTest {

    private lateinit var viewModel: SOSViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = SOSViewModel(context)
    }

    @Test
    fun `initial state is inactive`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isSosActivating)
        assertFalse(state.isSosActive)
        assertEquals(0, state.sosActivationCountdown)
    }

    @Test
    fun `cancelSosActivation resets state`() = runTest {
        viewModel.cancelSosActivation()

        val state = viewModel.uiState.value
        assertFalse(state.isSosActivating)
        assertFalse(state.isSosActive)
        assertEquals(0, state.sosActivationCountdown)
    }

    @Test
    fun `deactivateSos resets state`() = runTest {
        viewModel.deactivateSos()

        val state = viewModel.uiState.value
        assertFalse(state.isSosActivating)
        assertFalse(state.isSosActive)
        assertEquals(0, state.sosActivationCountdown)
    }
}

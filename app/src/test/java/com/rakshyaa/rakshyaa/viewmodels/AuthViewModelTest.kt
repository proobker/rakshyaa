package com.rakshyaa.rakshyaa.viewmodels

import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.auth.AuthState
import com.rakshyaa.rakshyaa.data.sync.AppDataSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var appDataSync: AppDataSync
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = mock(AuthRepository::class.java)
        Mockito.`when`(authRepository.state)
            .thenReturn(MutableStateFlow(AuthState()).asStateFlow())
        appDataSync = mock(AppDataSync::class.java)
        viewModel = AuthViewModel(authRepository, appDataSync)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signInWithGoogle delegates to AuthRepository`() = runTest(dispatcher) {
        viewModel.signInWithGoogle()
        Mockito.verify(authRepository).signInWithGoogle()
    }

    @Test
    fun `signOut delegates to AuthRepository`() = runTest(dispatcher) {
        viewModel.signOut()
        Mockito.verify(authRepository).signOut()
    }

    @Test
    fun `refreshUser delegates to AuthRepository`() = runTest(dispatcher) {
        viewModel.refreshUser()
        Mockito.verify(authRepository).fetchMe()
    }
}
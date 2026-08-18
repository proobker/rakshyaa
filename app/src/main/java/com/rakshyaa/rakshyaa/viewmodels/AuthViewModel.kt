package com.rakshyaa.rakshyaa.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for handling authentication UI state
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isAuthenticated: Boolean = false,
        val email: String = "",
        val password: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Auth state flow from repository
    init {
        viewModelScope.launch {
            authRepository.authState
                .mapLatest { session ->
                    _uiState.update { it.copy(
                        isAuthenticated = session != null,
                        errorMessage = null
                    ) }
                }
                .launchIn(viewModelScope)
        }
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            ) }

            val result = authRepository.signInWithEmail(email, password)

            _uiState.update { it.copy(
                isLoading = false
            ) }

            when (result) {
                is androidx.core.util.Result.Success -> {
                    // Sign in successful, auth state will be updated via flow
                }
                is androidx.core.util.Result.Failure -> {
                    _uiState.update { it.copy(
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                            ?: "Sign in failed"
                    ) }
                }
            }
        }
    }

    /**
     * Sign up with email and password
     */
    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            ) }

            val result = authRepository.signUpWithEmail(email, password)

            _uiState.update { it.copy(
                isLoading = false
            ) }

            when (result) {
                is androidx.core.util.Result.Success -> {
                    // Sign up successful, auth state will be updated via flow
                }
                is androidx.core.util.Result.Failure -> {
                    _uiState.update { it.copy(
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                            ?: "Sign up failed"
                    ) }
                }
            }
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            ) }

            val result = authRepository.signOut()

            _uiState.update { it.copy(
                isLoading = false
            ) }

            when (result) {
                is androidx.core.util.Result.Success -> {
                    // Sign out successful
                }
                is androidx.core.util.Result.Failure -> {
                    _uiState.update { it.copy(
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                            ?: "Sign out failed"
                    ) }
                }
            }
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null
            ) }

            val result = authRepository.sendPasswordResetEmail(email)

            _uiState.update { it.copy(
                isLoading = false
            ) }

            when (result) {
                is androidx.core.util.Result.Success -> {
                    // Password reset email sent
                }
                is androidx.core.util.Result.Failure -> {
                    _uiState.update { it.copy(
                        errorMessage = result.exceptionOrNull()?.localizedMessage
                            ?: "Failed to send reset email"
                    ) }
                }
            }
        }
    }

    /**
     * Update email field
     */
    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    /**
     * Update password field
     */
    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    /**
     * Clear form fields
     */
    fun clearForm() {
        _uiState.update { it.copy(
            email = "",
            password = "",
            errorMessage = null
        ) }
    }

    companion object {
        /** Factory for creating AuthViewModel instances */
        @Singleton
        class Factory @Inject constructor(
            private val authRepository: AuthRepository
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(authRepository) as T
            }
        }
    }
}
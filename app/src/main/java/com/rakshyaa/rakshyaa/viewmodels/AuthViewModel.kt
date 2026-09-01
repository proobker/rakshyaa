package com.rakshyaa.rakshyaa.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.data.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that surfaces the authentication state to the UI and delegates
 * Google sign-in / sign-out to the [AuthRepository].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.state

    private val _signingOut = MutableStateFlow(false)
    val signingOut: StateFlow<Boolean> = _signingOut.asStateFlow()

    fun signInWithGoogle() {
        viewModelScope.launch { authRepository.signInWithGoogle() }
    }

    fun refreshUser() {
        viewModelScope.launch { authRepository.fetchMe() }
    }

    fun signOut() {
        viewModelScope.launch {
            _signingOut.value = true
            authRepository.signOut()
            _signingOut.value = false
        }
    }

    fun clearError() {
        // Errors live in the repository state; nothing to clear locally.
    }
}

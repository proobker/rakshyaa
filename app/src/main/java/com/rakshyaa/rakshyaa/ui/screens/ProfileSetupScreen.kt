package com.rakshyaa.rakshyaa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.keyboard.TYPE.EMAIL_ADDRESS
import androidx.compose.ui.input.keyboard.TYPE.PHONE
import androidx.compose.ui.input.keyboard.TYPE.TEXT
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.data.auth.AuthRepository
import com.rakshyaa.rakshyaa.theme.RakshyaaTheme
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import hiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Profile setup screen for completing user profile after signup
 */
@AndroidEntryPoint
class ProfileSetupActivity : ComponentActivity() {

    private val viewModel: ProfileSetupViewModel by hiltViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshyaaTheme {
                ProfileSetupScreen(
                    onNavigateToHome = { /* Handle navigation to home */ }
                )
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    onNavigateToHome: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)

        ProfileSetupFormContent(
            uiState = uiState,
            onFirstNameChanged = { viewModel.updateFirstName(it) },
            onLastNameChanged = { viewModel.updateLastName(it) },
            onPhoneChanged = { viewModel.updatePhone(it) },
            onSaveProfileClicked = {
                viewModel.saveProfile()
            },
            onNavigateToHome = onNavigateToHome,
            modifier = contentModifier
        )
    }
}

@Composable
private fun ProfileSetupFormContent(
    uiState: ProfileSetupScreen.UiState,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onSaveProfileClicked: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo / Icon
        Icon(
            imageVector = Icons.Default.ShieldCheck,
            contentDescription = "Rakshyaa app logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        // Welcome Text
        Text(
            text = stringResource(R.string.profile_setup_welcome),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.profile_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 32.dp)
        )

        // First Name Field
        OutlinedTextField(
            value = uiState.firstName,
            onValueChange = { onFirstNameChanged(it) },
            label = { Text(stringResource(R.string.first_name)) },
            placeholder = { Text(stringResource(R.string.enter_first_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Last Name Field
        OutlinedTextField(
            value = uiState.lastName,
            onValueChange = { onLastNameChanged(it) },
            label = { Text(stringResource(R.string.last_name)) },
            placeholder = { Text(stringResource(R.string.enter_last_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Phone Number Field
        OutlinedTextField(
            value = uiState.phone,
            onValueChange = { onPhoneChanged(it) },
            label = { Text(stringResource(R.string.phone_number)) },
            placeholder = { Text(stringResource(R.string.enter_phone_number)) },
            keyboardOptions = KeyboardOptions(keyboardType = TYPE.PHONE),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Profile Button
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        } else {
            Button(
                onClick = onSaveProfileClicked,
                enabled = uiState.firstName.isNotBlank() && uiState.lastName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_profile))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Skip for now option
        TextButton(
            onClick = onNavigateToHome,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.skip_for_now))
        }
    }
}

/**
 * ViewModel for handling profile setup UI state
 */
class ProfileSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val firstName: String = "",
        val lastName: String = "",
        val phone: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Update first name field
     */
    fun updateFirstName(firstName: String) {
        _uiState.update { it.copy(firstName = firstName) }
    }

    /**
     * Update last name field
     */
    fun updateLastName(lastName: String) {
        _uiState.update { it.copy(lastName = lastName) }
    }

    /**
     * Update phone field
     */
    fun updatePhone(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    /**
     * Save user profile
     */
    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            ) }

            // In a real implementation, we would save the profile to Supabase
            // For now, we'll simulate success
            try {
                // TODO: Implement actual profile saving to Supabase
                // val user = authRepository.currentUser
                // if (user != null) {
                //     // Update user profile in user_profiles table
                // }

                // Simulate delay
                Thread.sleep(1000)

                _uiState.update { it.copy(
                    isLoading = false,
                    successMessage = "Profile saved successfully"
                ) }

                // Navigate to home after a short delay
                viewModelScope.launch {
                    delay(1500)
                    // TODO: Implement navigation to home
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = "Failed to save profile: ${e.localizedMessage}"
                ) }
            }
        }
    }

    companion object {
        /** Factory for creating ProfileSetupViewModel instances */
        @Inject
        class Factory @Inject constructor(
            private val authRepository: AuthRepository
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileSetupViewModel(authRepository) as T
            }
        }
    }
}
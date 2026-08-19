package com.rakshyaa.rakshyaa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.keyboard.TYPE.EMAIL_ADDRESS
import androidx.compose.ui.input.keyboard.TYPE.PASSWORD
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.theme.RakshyaaTheme
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import hiltViewModel
import java.util.regex.Pattern

/**
 * Signup screen for new user registration
 */
@AndroidEntryPoint
class SignupActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by hiltViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshyaaTheme {
                SignupScreen(
                    onNavigateToLogin = { /* Handle navigation to login */ }
                )
            }
        }
    }
}

@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
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

        SignupFormContent(
            uiState = uiState,
            onEmailChanged = { viewModel.updateEmail(it) },
            onPasswordChanged = { viewModel.updatePassword(it) },
            onSignUpClicked = {
                if (uiState.email.isNotBlank() && uiState.password.isNotBlank()) {
                    viewModel.signUpWithEmail(uiState.email, uiState.password)
                }
            },
            onNavigateToLogin = onNavigateToLogin,
            modifier = contentModifier
        )
    }
}

@Composable
private fun SignupFormContent(
    uiState: SignupScreen.UiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignUpClicked: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
            text = stringResource(R.string.signup_welcome),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.signup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 32.dp)
        )

        // Email Field
        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onEmailChanged(it) },
            label = { Text(stringResource(R.string.email)) },
            placeholder = { Text(stringResource(R.string.enter_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = TYPE.EMAIL_ADDRESS),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            isError = uiState.email.isNotBlank() && !isValidEmail(uiState.email),
            supportingText = if (uiState.email.isNotBlank() && !isValidEmail(uiState.email)) {
                Text(stringResource(R.string.invalid_email))
            } else null
        )

        // Password Field
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { onPasswordChanged(it) },
            label = { Text(stringResource(R.string.password)) },
            placeholder = { Text(stringResource(R.string.enter_password)) },
            leadingIcon = {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Password visibility icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = TYPE.PASSWORD),
            singleLine = true,
            password = !passwordVisible,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        // Confirm Password Field
        var confirmPasswordVisible by remember { mutableStateOf(false) }
        var confirmPassword by remember { mutableStateOf("") }
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password)) },
            placeholder = { Text(stringResource(R.string.confirm_password_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Confirm password visibility icon",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = TYPE.PASSWORD),
            singleLine = true,
            password = !confirmPasswordVisible,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            isError = confirmPassword.isNotBlank() && confirmPassword != uiState.password,
            supportingText = if (confirmPassword.isNotBlank() && confirmPassword != uiState.password) {
                Text(stringResource(R.string.passwords_do_not_match))
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign Up Button
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        } else {
            Button(
                onClick = onSignUpClicked,
                enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank() &&
                        confirmPassword.isNotBlank() && confirmPassword == uiState.password,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_up))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Already have account? Login
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.already_have_account))
            TextButton(onClick = onNavigateToLogin) {
                Text(stringResource(R.string.sign_in))
            }
        }
    }
}

/**
 * Email validation pattern
 */
private fun isValidEmail(email: String): Boolean {
    val emailPattern = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    )
    return emailPattern.matcher(email).matches()
}
package com.rakshyaa.rakshyaa.ui.screens

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.validation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.bitmap.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.keyboard.TYPE.EMAIL_ADDRESS
import androidx.compose.ui.input.keyboard.TYPE.PASSWORD
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.loadengine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.theme.RakshyaaTheme
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import hiltViewModel
import java.util.regex.Pattern

/**
 * Login screen for email/password authentication
 */
@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by hiltViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshyaaTheme {
                LoginScreen(
                    onNavigateToSignup = { /* Handle navigation to signup */ },
                    onNavigateToForgotPassword = { /* Handle navigation to forgot password */ }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
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

        LoginFormContent(
            uiState = uiState,
            onEmailChanged = { viewModel.updateEmail(it) },
            onPasswordChanged = { viewModel.updatePassword(it) },
            onSignInClicked = {
                if (uiState.email.isNotBlank() && uiState.password.isNotBlank()) {
                    viewModel.signInWithEmail(uiState.email, uiState.password)
                }
            },
            onNavigateToSignup = onNavigateToSignup,
            onNavigateToForgotPassword = onNavigateToForgotPassword,
            modifier = contentModifier
        )
    }
}

@Composable
private fun LoginFormContent(
    uiState: LoginScreen.UiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClicked: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
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
            text = stringResource(R.string.login_welcome),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.login_subtitle),
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

        // Forgot Password
        TextButton(
            onClick = onNavigateToForgotPassword,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.forgot_password))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Button
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        } else {
            Button(
                onClick = onSignInClicked,
                enabled = uiState.email.isNotBlank() && uiState.password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sign_in))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Line(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.or),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(24.dp)
            )
            Line(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Social Login Buttons (placeholder for future implementation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spaceEvenly
        ) {
            SocialLoginButton(
                icon = Icons.Default.G@@@android:drawable/sym_def_app_icon,
                label = stringResource(R.string.google),
                onClick = { /* TODO: Implement Google sign in */ }
            )
            SocialLoginButton(
                icon = Icons.Default.Facebook,
                label = stringResource(R.string.facebook),
                onClick = { /* TODO: Implement Facebook sign in */ }
            )
            SocialLoginButton(
                icon = Icons.Default.Apple,
                label = stringResource(R.string.apple),
                onClick = { /* TODO: Implement Apple sign in */ }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Don't have account? Sign up
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.dont_have_account))
            TextButton(onClick = onNavigateToSignup) {
                Text(stringResource(R.string.sign_up))
            }
        }
    }
}

@Composable
private fun Line(modifier: Modifier = Modifier) = Divider(
    modifier = modifier
        .fillMaxWidth()
        .height(1.dp),
    color = MaterialTheme.colorScheme.outlineVariant
)

@Composable
private fun SocialLoginButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .size(60.dp)
            .shape = MaterialTheme.shapes.small
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
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
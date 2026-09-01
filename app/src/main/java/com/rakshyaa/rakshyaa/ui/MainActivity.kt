package com.rakshyaa.rakshyaa.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.rakshyaa.rakshyaa.ui.screens.HomeScreen
import com.rakshyaa.rakshyaa.ui.screens.LoginScreen
import com.rakshyaa.rakshyaa.ui.theme.RakshyaaTheme
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshyaaTheme {
                RakshyaaApp()
            }
        }
    }
}

@Composable
fun RakshyaaApp(viewModel: AuthViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsState()

    if (authState.isLoggedIn) {
        LaunchedEffect(authState.isLoggedIn) {
            viewModel.refreshUser()
        }
    }

    Scaffold { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (authState.isLoggedIn) {
                HomeScreen(
                    userEmail = authState.user?.email,
                    onSignOut = { viewModel.signOut() }
                )
            } else {
                LoginScreen()
            }
        }
    }
}

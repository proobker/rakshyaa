package com.rakshyaa.rakshyaa.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rakshyaa.rakshyaa.ui.components.BottomNavBar
import com.rakshyaa.rakshyaa.ui.navigation.RakshyaaNavHost
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
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute by remember(currentBackStackEntry) {
        derivedStateOf { currentBackStackEntry?.destination?.route ?: "home" }
    }

    if (authState.isLoggedIn) {
        androidx.compose.runtime.LaunchedEffect(authState.isLoggedIn) {
            viewModel.refreshUser()
        }
    }

    Scaffold(
        bottomBar = {
            if (authState.isLoggedIn) {
                BottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (authState.isLoggedIn) {
                RakshyaaNavHost(navController)
            } else {
                LoginScreen()
            }
        }
    }
}

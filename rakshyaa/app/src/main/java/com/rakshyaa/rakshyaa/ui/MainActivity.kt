package com.rakshyaa.rakshyaa.ui

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rakshyaa.rakshyaa.ui.components.BottomNavBar
import com.rakshyaa.rakshyaa.ui.navigation.RakshyaaNavHost
import com.rakshyaa.rakshyaa.ui.screens.LoginScreen
import com.rakshyaa.rakshyaa.ui.theme.RakshyaaTheme
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val updateDownloaded = mutableStateOf(false)
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) updateDownloaded.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkForPlayUpdate()
        setContent {
            RakshyaaTheme {
                RakshyaaApp(
                    updateDownloaded = updateDownloaded.value,
                    onRestartForUpdate = { appUpdateManager.completeUpdate() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // A flexible update can finish downloading while the app is in the background.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) updateDownloaded.value = true
        }
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(installListener)
        super.onDestroy()
    }

    /** Offers Play's flexible update prompt; the app stays usable while it downloads. */
    private fun checkForPlayUpdate() {
        appUpdateManager.registerListener(installListener)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }
}

@Composable
fun RakshyaaApp(
    viewModel: AuthViewModel = hiltViewModel(),
    updateDownloaded: Boolean = false,
    onRestartForUpdate: () -> Unit = {}
) {
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

    val snackbarHostState = remember { SnackbarHostState() }
    val updateMessage = stringResource(com.rakshyaa.rakshyaa.R.string.update_downloaded)
    val updateAction = stringResource(com.rakshyaa.rakshyaa.R.string.update_restart)
    LaunchedEffect(updateDownloaded) {
        if (updateDownloaded) {
            val result = snackbarHostState.showSnackbar(
                message = updateMessage,
                actionLabel = updateAction,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) onRestartForUpdate()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (authState.isLoggedIn) {
                BottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // A tab already in the back stack (e.g. Home under a feature screen) is
                        // popped to directly; restoring its saved state would reopen that screen.
                        if (!navController.popBackStack(route, inclusive = false)) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
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

package com.rakshyaa.rakshyaa.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rakshyaa.rakshyaa.ui.screens.CheckInScreen
import com.rakshyaa.rakshyaa.ui.screens.EmergencyContactsScreen
import com.rakshyaa.rakshyaa.ui.screens.FakeCallScreen
import com.rakshyaa.rakshyaa.ui.screens.HomeScreen
import com.rakshyaa.rakshyaa.ui.screens.LegalHelpScreen
import com.rakshyaa.rakshyaa.ui.screens.LocationTrackingScreen
import com.rakshyaa.rakshyaa.ui.screens.LoginScreen
import com.rakshyaa.rakshyaa.ui.screens.ProfileScreen
import com.rakshyaa.rakshyaa.ui.screens.RideMonitoringScreen
import com.rakshyaa.rakshyaa.ui.screens.SafePlacesScreen
import com.rakshyaa.rakshyaa.ui.screens.SOSScreen
import com.rakshyaa.rakshyaa.ui.screens.VideoCaptureScreen
import com.rakshyaa.rakshyaa.viewmodels.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel

sealed interface Screen {
    val route: String
    val label: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector

    @Composable
    fun render(navController: NavHostController, authViewModel: AuthViewModel)
}

abstract class BaseScreen(
    override val route: String,
    override val label: String,
    override val icon: androidx.compose.ui.graphics.vector.ImageVector,
    override val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) : Screen {
    @Composable
    override abstract fun render(navController: NavHostController, authViewModel: AuthViewModel)
}

object HomeScreen : BaseScreen(
    route = "home",
    label = "Home",
    icon = Icons.Default.Home,
    selectedIcon = Icons.Filled.Home
) {
    @Composable
    override fun render(navController: NavHostController, authViewModel: AuthViewModel) {
        com.rakshyaa.rakshyaa.ui.screens.HomeScreen(
            userEmail = authViewModel.authState.value.user?.email,
            onSignOut = { authViewModel.signOut() },
            onNavigate = { route -> navController.navigate(route) }
        )
    }
}

object SOSScreen : BaseScreen(
    route = "sos",
    label = "SOS",
    icon = Icons.Default.Shield,
    selectedIcon = Icons.Filled.Shield
) {
    @Composable
    override fun render(navController: NavHostController, authViewModel: AuthViewModel) {
        SOSScreen()
    }
}

object TrackingScreen : BaseScreen(
    route = "tracking",
    label = "Track",
    icon = Icons.Default.LocationOn,
    selectedIcon = Icons.Filled.LocationOn
) {
    @Composable
    override fun render(navController: NavHostController, authViewModel: AuthViewModel) {
        LocationTrackingScreen(onNavigate = { route -> navController.navigate(route) })
    }
}

object ContactsScreen : BaseScreen(
    route = "contacts",
    label = "Contacts",
    icon = Icons.Default.Person,
    selectedIcon = Icons.Filled.Person
) {
    @Composable
    override fun render(navController: NavHostController, authViewModel: AuthViewModel) {
        EmergencyContactsScreen(onNavigate = { route -> navController.navigate(route) })
    }
}

object MoreScreen : BaseScreen(
    route = "more",
    label = "More",
    icon = Icons.Default.Menu,
    selectedIcon = Icons.Filled.Menu
) {
    @Composable
    override fun render(navController: NavHostController, authViewModel: AuthViewModel) {
        // Placeholder for More screen with sub-navigation
        // Sub-routes: ride, checkin, video, safeplaces, legal, fakecall, profile
        androidx.compose.material3.Text(text = "More Screen - Coming Soon")
    }
}

val screens = listOf(
    HomeScreen,
    SOSScreen,
    TrackingScreen,
    ContactsScreen,
    MoreScreen
)

@Composable
fun RakshyaaNavHost(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen.render(navController, authViewModel)
        }
        composable("sos") {
            SOSScreen.render(navController, authViewModel)
        }
        composable("tracking") {
            TrackingScreen.render(navController, authViewModel)
        }
        composable("contacts") {
            ContactsScreen.render(navController, authViewModel)
        }
        composable("more") {
            MoreScreen.render(navController, authViewModel)
        }
        
        // Sub-routes under more (will be implemented in later phases)
        composable("ride") {
            RideMonitoringScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("checkin") {
            CheckInScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("video") {
            VideoCaptureScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("safeplaces") {
            SafePlacesScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("legal") {
            LegalHelpScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("fakecall") {
            FakeCallScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable("profile") {
            ProfileScreen(onSignOut = { authViewModel.signOut() })
        }
    }
}
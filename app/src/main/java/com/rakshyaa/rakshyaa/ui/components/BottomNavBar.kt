package com.rakshyaa.rakshyaa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshyaa.rakshyaa.ui.navigation.screens

@Composable
fun BottomNavBar(
    navController: androidx.navigation.NavHostController,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val screensList = screens
    val selectedIndex = screensList.indexOfFirst { currentRoute.startsWith(it.route) }

    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val onSurfaceVariant = colors.onSurfaceVariant
    val surfaceColor = colors.surfaceContainerHighest

    NavigationBar(
        containerColor = surfaceColor.copy(alpha = 0.95f)
    ) {
        screensList.forEachIndexed { index, screen ->
            val isSelected = index == selectedIndex

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            val iconAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1.0f else 0.7f
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen.route) },
                icon = {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = iconAlpha
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.icon,
                            contentDescription = screen.label,
                            tint = if (isSelected) primaryColor else onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = screen.label,
                        color = if (isSelected) primaryColor else onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Medium else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryColor,
                    unselectedIconColor = onSurfaceVariant,
                    selectedTextColor = primaryColor,
                    unselectedTextColor = onSurfaceVariant,
                    indicatorColor = primaryColor.copy(alpha = 0.12f)
                )
            )
        }
    }
}
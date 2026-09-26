package com.rakshyaa.rakshyaa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshyaa.rakshyaa.R

@Composable
fun HomeScreen(
    userEmail: String?,
    onNavigate: (String) -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val colors = MaterialTheme.colorScheme
    val primaryColor = colors.primary
    val onSurface = colors.onSurface
    val onSurfaceVariant = colors.onSurfaceVariant
    val surfaceContainer = colors.surfaceContainer
    val errorColor = colors.error
    val tertiaryColor = colors.tertiary

    val features = listOf(
        FeatureItem(
            title = stringResource(R.string.home_sos),
            description = stringResource(R.string.home_sos_desc),
            icon = Icons.Default.Shield,
            color = errorColor,
            route = "sos",
            isPrimary = true
        ),
        FeatureItem(
            title = stringResource(R.string.home_track),
            description = stringResource(R.string.home_track_desc),
            icon = Icons.Default.LocationOn,
            color = primaryColor,
            route = "tracking"
        ),
        FeatureItem(
            title = stringResource(R.string.home_contacts),
            description = stringResource(R.string.home_contacts_desc),
            icon = Icons.Default.Person,
            color = tertiaryColor,
            route = "contacts"
        ),
        FeatureItem(
            title = stringResource(R.string.home_safe_places),
            description = stringResource(R.string.home_safe_places_desc),
            icon = Icons.Default.LocalHospital,
            color = primaryColor,
            route = "safeplaces"
        ),
        FeatureItem(
            title = stringResource(R.string.home_legal),
            description = stringResource(R.string.home_legal_desc),
            icon = Icons.Default.Gavel,
            color = tertiaryColor,
            route = "legal"
        ),
        FeatureItem(
            title = stringResource(R.string.home_ride_monitoring),
            description = stringResource(R.string.home_ride_monitoring_desc),
            icon = Icons.Default.DirectionsCar,
            color = primaryColor,
            route = "ride"
        ),
        FeatureItem(
            title = stringResource(R.string.home_check_ins),
            description = stringResource(R.string.home_check_ins_desc),
            icon = Icons.Default.EmojiEvents,
            color = tertiaryColor,
            route = "checkin"
        ),
        FeatureItem(
            title = stringResource(R.string.home_video_capture),
            description = stringResource(R.string.home_video_capture_desc),
            icon = Icons.Default.Videocam,
            color = primaryColor,
            route = "video"
        ),
        FeatureItem(
            title = stringResource(R.string.home_fake_call),
            description = stringResource(R.string.home_fake_call_desc),
            icon = Icons.Default.Phone,
            color = tertiaryColor,
            route = "fakecall"
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.brand_logo),
                    contentDescription = null, modifier = Modifier.size(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.home_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant
                    )
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                onClick = { onNavigate("sos") },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = colors.errorContainer,
                    contentColor = colors.onErrorContainer
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, null, modifier = Modifier.size(40.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.home_sos_action), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.home_sos_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
        items(features.filterNot { it.isPrimary }, key = { it.route }) { feature ->
            FeatureCard(feature = feature, onClick = { onNavigate(feature.route) })
        }
    }
}

data class FeatureItem(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val route: String,
    val isPrimary: Boolean = false
)

@Composable
fun FeatureCard(
    feature: FeatureItem,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val onSurface = colors.onSurface
    val onSurfaceVariant = colors.onSurfaceVariant
    val surfaceContainer = colors.surfaceContainer
    
Card(
        onClick = onClick,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isPrimary) feature.color.copy(alpha = 0.08f) else surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isPrimary) 140.dp else 120.dp),
        shape = androidx.compose.material3.MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Icon with colored background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isPrimary) 56.dp else 48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(feature.color.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = feature.color,
                        modifier = Modifier.size(if (isPrimary) 28.dp else 24.dp)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = onSurface,
                        fontWeight = if (isPrimary) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
            }
            
            // Primary indicator
            if (isPrimary) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(feature.color)
                )
            }
        }
    }
}

package com.rakshyaa.rakshyaa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "Profile & Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface
        )
        Text(
            text = "Account settings, backup, notifications, about",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = colors.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Coming Soon", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Text("User profile, backup sync, notification toggles, about/legal", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = colors.errorContainer,
                contentColor = colors.onErrorContainer
            )
        ) {
            Text("Sign Out")
        }
    }
}



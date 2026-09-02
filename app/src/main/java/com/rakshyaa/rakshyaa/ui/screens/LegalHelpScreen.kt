package com.rakshyaa.rakshyaa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
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
fun LegalHelpScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Gavel,
            contentDescription = null,
            tint = Color(0xFF673AB7),
            modifier = Modifier.size(72.dp)
        )
        Text(
            text = "Legal Help",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface
        )
        Text(
            text = "Offline access to legal resources and helpline numbers",
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
                Text("Categorized legal resources, offline-first, emergency helplines", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}



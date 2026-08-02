package com.rakshyaa.rakshyaa.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rakshyaa.rakshyaa.R
import com.rakshyaa.rakshyaa.theme.RakshyaaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakshyaaTheme {
                MyApp()
            }
        }
    }
}

@Composable
fun MyApp() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rakshyaa") })
        }
    ) { padding ->
        // Main content area - will be replaced with actual screens later
        HomeScreen(modifier = Modifier
            .fillMaxSize()
            .padding(padding))
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // Placeholder for home screen - will be implemented later
}
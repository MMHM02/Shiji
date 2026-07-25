package com.shiji.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shiji.app.navigation.ShiJiNavGraph
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity host for 食记.
 * All screens are Composable destinations managed by Compose Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiJiNavGraph()
        }
    }
}

package com.studio.one_day_pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.studio.one_day_pomodoro.presentation.navigation.PomoNavHost
import com.studio.one_day_pomodoro.presentation.ui.components.ads.BannerAdView
import com.studio.one_day_pomodoro.presentation.ui.theme.PomoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomoTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { BannerAdView() }
                ) { paddingValues ->
                    PomoNavHost(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
package com.studio.one_day_pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.studio.one_day_pomodoro.domain.model.TimerMode
import com.studio.one_day_pomodoro.domain.repository.TimerStateRepository
import com.studio.one_day_pomodoro.presentation.navigation.PomoNavHost
import com.studio.one_day_pomodoro.presentation.navigation.Screen
import com.studio.one_day_pomodoro.presentation.ui.components.ads.BannerAdView
import com.studio.one_day_pomodoro.presentation.ui.theme.PomoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var timerRepository: TimerStateRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomoTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val isRunning = timerRepository.isRunning.first()
                    val mode = timerRepository.timerMode.first()
                    
                    // 1. Check intent from notification
                    val intent = (this@MainActivity as? android.app.Activity)?.intent
                    val finishedModeName = intent?.getStringExtra("TIMER_FINISHED_MODE")
                    
                    // 2. Check repository state for 'Just Finished' (Process Alive case)
                    val repoSeconds = timerRepository.remainingSeconds.first()
                    val repoMode = timerRepository.timerMode.first()

                    startDestination = if (isRunning) {
                        when (mode) {
                            TimerMode.BREAK -> "break_screen/25/0/0"
                            else -> "timer_screen/OTHERS" // Default
                        }
                    } else {
                        // Not running. Check if we just finished.
                        val isFocusFinished = (finishedModeName == TimerMode.FOCUS.name) || 
                                              (repoSeconds == 0L && repoMode == TimerMode.FOCUS)
                        
                        val isBreakFinished = (finishedModeName == TimerMode.BREAK.name) || 
                                              (repoSeconds == 0L && repoMode == TimerMode.BREAK)
                        
                        if (isFocusFinished) {
                             // Focus finished -> Go to Break
                             "break_screen/25/0/0"
                        } else if (isBreakFinished) {
                             // Break finished -> Go to Timer
                             "timer_screen/OTHERS"
                        } else {
                             Screen.Home.route
                        }
                    }
                }

                if (startDestination != null) {
                    Scaffold(
                        bottomBar = { BannerAdView() }
                    ) { paddingValues ->
                        PomoNavHost(
                            navController = navController,
                            startDestination = startDestination!!,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                } else {
                     Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}
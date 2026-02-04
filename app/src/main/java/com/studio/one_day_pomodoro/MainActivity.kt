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
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var timerRepository: TimerStateRepository

    private val _currentIntent = MutableStateFlow<android.content.Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _currentIntent.value = intent
        setContent {
            PomoTheme {
                val navController = rememberNavController()
                val isRunning by timerRepository.isRunning.collectAsState(initial = false)
                val mode by timerRepository.timerMode.collectAsState(initial = TimerMode.NONE)
                val currentIntentState by _currentIntent.collectAsState()
                
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(isRunning, mode, currentIntentState) {
                    val intent = currentIntentState
                    val finishedModeName = intent?.getStringExtra("TIMER_FINISHED_MODE")
                    
                    val repoSeconds = timerRepository.remainingSeconds.value
                    val repoMode = timerRepository.timerMode.value

                    val focusDuration = timerRepository.focusDurationMinutes.value
                    val completed = timerRepository.completedSessions.value
                    val total = timerRepository.totalSessions.value
                    val purpose = timerRepository.currentPurpose.value

                    val destination = if (isRunning) {
                        when (mode) {
                            TimerMode.BREAK -> Screen.Break.createRoute(focusDuration, completed, total)
                            else -> Screen.Timer.createRoute(purpose)
                        }
                    } else {
                        val isFocusFinished = (finishedModeName == TimerMode.FOCUS.name) || 
                                              (repoSeconds == 0L && repoMode == TimerMode.FOCUS)
                        
                        val isBreakFinished = (finishedModeName == TimerMode.BREAK.name) || 
                                              (repoSeconds == 0L && repoMode == TimerMode.BREAK)
                        
                        if (isFocusFinished) {
                             Screen.Break.createRoute(focusDuration, completed, total)
                        } else if (isBreakFinished) {
                             Screen.Timer.createRoute(purpose)
                        } else {
                             Screen.Home.route
                        }
                    }
                    
                    if (startDestination == null) {
                        startDestination = destination
                    } else {
                        // Guard Logic: 실행 중인데 엉뚱한 화면일 경우 강제 복귀
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (isRunning && currentRoute != null) {
                            val targetRoutePart = if (mode == TimerMode.BREAK) "break" else "timer"
                            if (!currentRoute.contains(targetRoutePart, ignoreCase = true)) {
                                navController.navigate(destination) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
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

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _currentIntent.value = intent
    }
}
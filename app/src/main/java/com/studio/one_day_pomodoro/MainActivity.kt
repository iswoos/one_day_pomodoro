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
                val isInitialized by timerRepository.isInitialized.collectAsState(initial = false)
                val currentIntentState by _currentIntent.collectAsState()
                
                var startDestination by remember { mutableStateOf<String?>(null) }

                // 초기 경로 결정 및 상태 변화 감지 (Guard Logic)
                LaunchedEffect(isInitialized, isRunning, mode, currentIntentState) {
                    if (!isInitialized) return@LaunchedEffect
                    
                    val intent = currentIntentState
                    val finishedModeName = intent?.getStringExtra("TIMER_FINISHED_MODE")
                    
                    val repoMode = timerRepository.timerMode.value
                    val repoSeconds = timerRepository.remainingSeconds.value
                    val completed = timerRepository.completedSessions.value
                    val total = timerRepository.totalSessions.value
                    val focusDuration = timerRepository.focusDurationMinutes.value
                    val purpose = timerRepository.currentPurpose.value

                    // 현재 상태에 기반한 이상적인 목적지 계산
                    val targetDestination = if (isRunning) {
                        when (mode) {
                            TimerMode.BREAK -> Screen.Break.createRoute(focusDuration, completed, total)
                            else -> Screen.Timer.createRoute(purpose)
                        }
                    } else {
                        val isLast = completed > 0 && completed >= total
                        
                        // 명시적 종료 상태 확인
                        val isFocusFinished = (finishedModeName == TimerMode.FOCUS.name) || 
                                              (repoSeconds == 0L && repoMode == TimerMode.FOCUS) ||
                                              (repoSeconds == 0L && isLast && repoMode == TimerMode.NONE)
                        
                        val isBreakFinished = (finishedModeName == TimerMode.BREAK.name) || 
                                              (repoSeconds == 0L && repoMode == TimerMode.BREAK)

                        // 일시정지 상태 확인 (실행 중은 아닌데, 모드는 있고 시간은 남은 경우)
                        val isPaused = (repoMode != TimerMode.NONE && repoSeconds > 0L)
                        
                        if (isFocusFinished) {
                             if (isLast) Screen.Summary.createRoute(purpose, completed * focusDuration)
                             else Screen.Break.createRoute(focusDuration, completed, total)
                        } else if (isBreakFinished) {
                             Screen.Timer.createRoute(purpose)
                        } else if (isPaused) {
                             // 일시정지 상태라면 해당 모드의 화면으로 복구
                             if (repoMode == TimerMode.BREAK) Screen.Break.createRoute(focusDuration, completed, total)
                             else Screen.Timer.createRoute(purpose)
                        } else {
                             Screen.Home.route
                        }
                    }
                    
                    // 1. 초기 목적지 설정
                    if (startDestination == null) {
                        startDestination = targetDestination
                    } 
                    
                    // 2. 가드 로직: 현재 화면이 상태와 맞지 않으면 강제 이동
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != null) {
                        // 기본 경로(base route) 비교를 위해 helper 사용
                        fun isSameBaseRoute(r1: String, r2: String): Boolean {
                            return r1.substringBefore("/") == r2.substringBefore("/")
                        }

                        val isActuallyInSession = isRunning || (repoMode != TimerMode.NONE && repoSeconds > 0L)
                        val isExplicitlyFinished = (intent?.hasExtra("TIMER_FINISHED_MODE") == true)
                        
                        // 현재 화면이 타겟과 다른 유형이거나, 세션 중인데 세션 외부 화면(Home 등)에 있는 경우 강제 이동
                        val isBaseRouteDifferent = !isSameBaseRoute(currentRoute, targetDestination)
                        val isOutsideButShouldBeIn = isActuallyInSession && !currentRoute.contains("timer", true) && !currentRoute.contains("break", true)

                        if (isExplicitlyFinished || isOutsideButShouldBeIn || (isActuallyInSession && isBaseRouteDifferent)) {
                            // 현재 경로와 타겟 경로가 정말로 다른지 최종 확인 후 이동
                            if (currentRoute != targetDestination) {
                                navController.navigate(targetDestination) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            
                            if (isExplicitlyFinished) {
                                _currentIntent.value = null
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
                            timerRepository = timerRepository,
                            startDestination = startDestination!!,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                } else {
                     // 로딩 대기 화면 (데이터 로드 전까지 빈 화면 방지 + 로딩 인디케이터)
                     Box(
                         modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                         contentAlignment = androidx.compose.ui.Alignment.Center
                     ) {
                         androidx.compose.material3.CircularProgressIndicator(
                             color = MaterialTheme.colorScheme.primary
                         )
                     }
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
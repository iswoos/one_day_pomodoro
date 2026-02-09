package com.studio.one_day_pomodoro.presentation.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.presentation.ui.components.ads.InterstitialAdHelper
import com.studio.one_day_pomodoro.presentation.ui.screens.home.HomeScreen
import com.studio.one_day_pomodoro.presentation.ui.screens.purpose.PurposeSelectScreen
import com.studio.one_day_pomodoro.presentation.ui.screens.timer.TimerScreen
import com.studio.one_day_pomodoro.presentation.ui.screens.pomo_break.BreakScreen
import com.studio.one_day_pomodoro.presentation.ui.screens.summary.SummaryScreen
import com.studio.one_day_pomodoro.presentation.ui.screens.settings.SettingsScreen

import com.studio.one_day_pomodoro.presentation.util.findActivity

@Composable
fun PomoNavHost(
    navController: NavHostController,
    timerRepository: com.studio.one_day_pomodoro.domain.repository.TimerStateRepository,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    startDestination: String = Screen.Home.route
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            // 홈 화면 진입 시 미리 로드 시작
            LaunchedEffect(Unit) {
                activity?.let { InterstitialAdHelper.loadAd(it) }
            }
            HomeScreen(
                onStartClick = { navController.navigate(Screen.PurposeSelect.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.PurposeSelect.route) {
            PurposeSelectScreen(
                onPurposeSelected = { purpose ->
                    navController.navigate(Screen.Timer.createRoute(purpose))
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Timer.route,
            arguments = listOf(navArgument("purpose") { type = NavType.StringType })
        ) { backStackEntry ->
            val purposeName = backStackEntry.arguments?.getString("purpose")
            val purpose = PomodoroPurpose.fromName(purposeName)
            
            TimerScreen(
                purpose = purpose,
                onStopClick = { 
                    // 확실하게 홈으로 이동 전 광고 노출
                    activity?.let {
                        InterstitialAdHelper.showAd(it) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } ?: run {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Break.route,
            arguments = listOf(
                navArgument("focusMinutes") { type = NavType.IntType },
                navArgument("completedSessions") { type = NavType.IntType },
                navArgument("totalSessions") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val focusMinutes = backStackEntry.arguments?.getInt("focusMinutes") ?: 25
            val completedSessions = backStackEntry.arguments?.getInt("completedSessions") ?: 0
            val totalSessions = backStackEntry.arguments?.getInt("totalSessions") ?: 0
            
            BreakScreen(
                focusMinutes = focusMinutes,
                completedSessions = completedSessions,
                totalSessions = totalSessions,
                onStopClick = { 
                    // 확실하게 홈으로 이동 전 광고 노출
                    activity?.let {
                        InterstitialAdHelper.showAd(it) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } ?: run {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Summary.route,
            arguments = listOf(
                navArgument("purpose") { type = NavType.StringType },
                navArgument("minutes") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val purpose = PomodoroPurpose.fromName(backStackEntry.arguments?.getString("purpose"))
            val minutes = backStackEntry.arguments?.getInt("minutes") ?: 0
            
            // 요약 화면 진입 시에도 보험용으로 로드 시도
            LaunchedEffect(Unit) {
                activity?.let { InterstitialAdHelper.loadAd(it) }
            }
            
            SummaryScreen(
                purpose = purpose,
                minutes = minutes,
                onConfirmClick = {
                    // 확인 시 이전 세션 상태 완전히 리셋
                    timerRepository.clearExpiredState()
                    
                    activity?.let {
                        InterstitialAdHelper.showAd(it) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    } ?: run {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onSaveClick = { 
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

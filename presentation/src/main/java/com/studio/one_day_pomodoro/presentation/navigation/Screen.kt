package com.studio.one_day_pomodoro.presentation.navigation

import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose

/**
 * 앱의 화면 이동 경로를 정의하는 봉인된 인터페이스입니다.
 */
sealed interface Screen {
    val route: String

    data object Home : Screen { override val route = "home" }
    data object PurposeSelect : Screen { override val route = "purpose_select" }
    
    data object Timer : Screen { 
        override val route = "timer/{purpose}"
        fun createRoute(purpose: PomodoroPurpose) = "timer/${purpose.name}"
    }
    
    data object Break : Screen {
        override val route = "break/{focusMinutes}"
        fun createRoute(focusMinutes: Int) = "break/$focusMinutes"
    }
    
    data object Summary : Screen { 
        override val route = "summary/{purpose}/{minutes}"
        fun createRoute(purpose: PomodoroPurpose, minutes: Int) = "summary/${purpose.name}/$minutes"
    }
    
    data object Settings : Screen { override val route = "settings" }
}

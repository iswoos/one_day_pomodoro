package com.studio.one_day_pomodoro.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PomodoroPrimary,
    secondary = PomodoroSecondary,
    background = PomodoroBackground,
    surface = PomodoroSurface,
    onPrimary = White,
    onSecondary = PomodoroText,
    onBackground = PomodoroText,
    onSurface = PomodoroText
)

@Composable
fun PomoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // 본 앱은 밝고 따뜻한 느낌을 위해 우선 라이트 테마를 기본으로 사용합니다.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

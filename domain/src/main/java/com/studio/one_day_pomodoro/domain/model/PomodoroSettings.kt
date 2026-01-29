package com.studio.one_day_pomodoro.domain.model

/**
 * 뽀모도로 타이머 및 반복 설정을 담는 데이터 클래스입니다.
 *
 * @property focusMinutes 집중 시간 (기본 25분)
 * @property breakMinutes 휴식 시간 (기본 5분)
 * @property repeatCount 반복 횟수 (기본 0회)
 */
data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val repeatCount: Int = 1
)

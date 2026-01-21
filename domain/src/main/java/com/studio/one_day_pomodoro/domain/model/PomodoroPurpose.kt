package com.studio.one_day_pomodoro.domain.model

/**
 * 뽀모도로 집중 목적의 유형을 정의하는 열거형 클래스입니다.
 */
enum class PomodoroPurpose(val displayName: String) {
    STUDY("공부"),
    DEV("개발"),
    READ("독서"),
    WORKOUT("운동"),
    MEDITATION("명상"),
    WORK("업무"),
    OTHERS("기타")
}

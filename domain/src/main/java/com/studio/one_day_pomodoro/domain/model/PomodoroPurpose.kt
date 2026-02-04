package com.studio.one_day_pomodoro.domain.model

/**
 * 뽀모도로 집중 목적의 유형을 정의하는 열거형 클래스입니다.
 */
enum class PomodoroPurpose(val displayName: String) {
    STUDY("공부"),
    READ("독서"),
    WORK("업무"),
    HEALTH("운동"),
    OTHERS("기타");

    companion object {
        fun fromName(name: String?): PomodoroPurpose {
            return entries.find { it.name == name } ?: OTHERS
        }
    }
}

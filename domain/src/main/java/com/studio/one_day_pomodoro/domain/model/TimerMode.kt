package com.studio.one_day_pomodoro.domain.model

enum class TimerMode {
    FOCUS,
    BREAK,
    NONE;

    companion object {
        fun fromName(name: String?): TimerMode {
            return entries.find { it.name == name } ?: NONE
        }
    }
}

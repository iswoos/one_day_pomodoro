package com.studio.one_day_pomodoro.domain.repository

import kotlinx.coroutines.flow.StateFlow

import com.studio.one_day_pomodoro.domain.model.TimerMode

interface TimerStateRepository {
    val remainingSeconds: StateFlow<Long>
    val isRunning: StateFlow<Boolean>
    val timerMode: StateFlow<TimerMode>
    
    val focusDurationMinutes: StateFlow<Int>
    val breakDurationMinutes: StateFlow<Int>
    val totalSessions: StateFlow<Int>
    val completedSessions: StateFlow<Int>
    val currentPurpose: StateFlow<com.studio.one_day_pomodoro.domain.model.PomodoroPurpose>
    
    fun start(
        seconds: Long, 
        mode: TimerMode, 
        focusMin: Int = 25, 
        breakMin: Int = 5, 
        total: Int = 1, 
        completed: Int = 0,
        purpose: com.studio.one_day_pomodoro.domain.model.PomodoroPurpose = com.studio.one_day_pomodoro.domain.model.PomodoroPurpose.OTHERS
    )
    fun updateSessionInfo(completed: Int, total: Int)
    fun pause()
    fun resume()
    fun stop()
}

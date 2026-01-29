package com.studio.one_day_pomodoro.domain.repository

import kotlinx.coroutines.flow.StateFlow

import com.studio.one_day_pomodoro.domain.model.TimerMode

interface TimerStateRepository {
    val remainingSeconds: StateFlow<Long>
    val isRunning: StateFlow<Boolean>
    val timerMode: StateFlow<TimerMode>
    
    fun start(seconds: Long, mode: TimerMode)
    fun pause()
    fun resume()
    fun stop()
}

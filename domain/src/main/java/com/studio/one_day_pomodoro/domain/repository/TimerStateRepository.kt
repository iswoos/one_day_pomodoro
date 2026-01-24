package com.studio.one_day_pomodoro.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface TimerStateRepository {
    val remainingSeconds: StateFlow<Long>
    val isRunning: StateFlow<Boolean>
    
    fun start(seconds: Long)
    fun pause()
    fun resume()
    fun stop()
}

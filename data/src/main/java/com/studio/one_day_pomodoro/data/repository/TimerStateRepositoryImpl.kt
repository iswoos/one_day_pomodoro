package com.studio.one_day_pomodoro.data.repository

import com.studio.one_day_pomodoro.domain.repository.TimerStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerStateRepositoryImpl @Inject constructor() : TimerStateRepository {

    private val _remainingSeconds = MutableStateFlow(0L)
    override val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _timerMode = MutableStateFlow(com.studio.one_day_pomodoro.domain.model.TimerMode.NONE)
    override val timerMode: StateFlow<com.studio.one_day_pomodoro.domain.model.TimerMode> = _timerMode.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var targetEndTimeMillis: Long = 0L

    override fun start(seconds: Long, mode: com.studio.one_day_pomodoro.domain.model.TimerMode) {
        _remainingSeconds.value = seconds
        _isRunning.value = true
        _timerMode.value = mode
        targetEndTimeMillis = SystemClock.elapsedRealtime() + (seconds * 1000)
        startTimerJob()
    }

    override fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    override fun resume() {
        if (_remainingSeconds.value > 0) {
            _isRunning.value = true
            targetEndTimeMillis = SystemClock.elapsedRealtime() + (_remainingSeconds.value * 1000)
            startTimerJob()
        }
    }

    override fun stop() {
        _isRunning.value = false
        _timerMode.value = com.studio.one_day_pomodoro.domain.model.TimerMode.NONE
        timerJob?.cancel()
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = repositoryScope.launch {
            while (isActive && _isRunning.value) {
                val now = SystemClock.elapsedRealtime()
                val remaining = ((targetEndTimeMillis - now + 999) / 1000).coerceAtLeast(0)
                
                _remainingSeconds.value = remaining
                
                if (remaining <= 0) {
                    _isRunning.value = false
                    break
                }
                
                delay(500)
            }
        }
    }
}

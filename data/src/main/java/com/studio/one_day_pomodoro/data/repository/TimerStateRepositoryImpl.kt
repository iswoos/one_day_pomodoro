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

    private val _focusDurationMinutes = MutableStateFlow(25)
    override val focusDurationMinutes: StateFlow<Int> = _focusDurationMinutes.asStateFlow()

    private val _breakDurationMinutes = MutableStateFlow(5)
    override val breakDurationMinutes: StateFlow<Int> = _breakDurationMinutes.asStateFlow()

    private val _totalSessions = MutableStateFlow(1)
    override val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _completedSessions = MutableStateFlow(0)
    override val completedSessions: StateFlow<Int> = _completedSessions.asStateFlow()

    private val _currentPurpose = MutableStateFlow(com.studio.one_day_pomodoro.domain.model.PomodoroPurpose.OTHERS)
    override val currentPurpose: StateFlow<com.studio.one_day_pomodoro.domain.model.PomodoroPurpose> = _currentPurpose.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var targetEndTimeMillis: Long = 0L

    override fun start(
        seconds: Long, 
        mode: com.studio.one_day_pomodoro.domain.model.TimerMode,
        focusMin: Int,
        breakMin: Int,
        total: Int,
        completed: Int,
        purpose: com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
    ) {
        _remainingSeconds.value = seconds
        _isRunning.value = true
        _timerMode.value = mode
        _focusDurationMinutes.value = focusMin
        _breakDurationMinutes.value = breakMin
        _totalSessions.value = total
        _completedSessions.value = completed
        _currentPurpose.value = purpose
        
        targetEndTimeMillis = SystemClock.elapsedRealtime() + (seconds * 1000)
        startTimerJob()
    }

    override fun updateSessionInfo(completed: Int, total: Int) {
        _completedSessions.value = completed
        _totalSessions.value = total
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

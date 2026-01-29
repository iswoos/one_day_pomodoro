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

    override fun start(seconds: Long, mode: com.studio.one_day_pomodoro.domain.model.TimerMode) {
        _remainingSeconds.value = seconds
        _isRunning.value = true
        _timerMode.value = mode
        startTimerJob()
    }

    override fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    override fun resume() {
        if (_remainingSeconds.value > 0) {
            _isRunning.value = true
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
            while (isActive && _remainingSeconds.value > 0 && _isRunning.value) {
                delay(1000)
                _remainingSeconds.value -= 1
                if (_remainingSeconds.value <= 0) {
                    _isRunning.value = false
                    // Timer finished, mode dictates notification content in Service
                }
            }
        }
    }
}

package com.studio.one_day_pomodoro.data.repository

import android.content.SharedPreferences
import android.os.SystemClock
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.domain.model.TimerMode
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
class TimerStateRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences
) : TimerStateRepository {

    private val _remainingSeconds = MutableStateFlow(0L)
    override val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.NONE)
    override val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _focusDurationMinutes = MutableStateFlow(25)
    override val focusDurationMinutes: StateFlow<Int> = _focusDurationMinutes.asStateFlow()

    private val _breakDurationMinutes = MutableStateFlow(5)
    override val breakDurationMinutes: StateFlow<Int> = _breakDurationMinutes.asStateFlow()

    private val _totalSessions = MutableStateFlow(1)
    override val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _completedSessions = MutableStateFlow(0)
    override val completedSessions: StateFlow<Int> = _completedSessions.asStateFlow()

    private val _currentPurpose = MutableStateFlow(PomodoroPurpose.OTHERS)
    override val currentPurpose: StateFlow<PomodoroPurpose> = _currentPurpose.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var targetEndTimeMillis: Long = 0L

    init {
        loadState()
    }

    override fun start(
        seconds: Long, 
        mode: TimerMode,
        focusMin: Int,
        breakMin: Int,
        total: Int,
        completed: Int,
        purpose: PomodoroPurpose
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
        saveState()
        startTimerJob()
    }

    override fun updateSessionInfo(completed: Int, total: Int) {
        _completedSessions.value = completed
        _totalSessions.value = total
        saveState()
    }

    override fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
        saveState()
    }

    override fun resume() {
        if (_remainingSeconds.value > 0) {
            _isRunning.value = true
            targetEndTimeMillis = SystemClock.elapsedRealtime() + (_remainingSeconds.value * 1000)
            saveState()
            startTimerJob()
        }
    }

    override fun stop() {
        _isRunning.value = false
        _timerMode.value = TimerMode.NONE
        timerJob?.cancel()
        _remainingSeconds.value = 0
        saveState()
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = repositoryScope.launch {
            while (isActive && _isRunning.value) {
                val now = SystemClock.elapsedRealtime()
                val remaining = ((targetEndTimeMillis - now + 999) / 1000).coerceAtLeast(0)
                
                if (remaining != _remainingSeconds.value) {
                    _remainingSeconds.value = remaining
                    // 대략 5초마다 한 번씩 저장하여 IO 부하 줄임 (앱 종료 시점 대비)
                    if (remaining % 5 == 0L) {
                        saveState()
                    }
                }
                
                if (remaining <= 0) {
                    _isRunning.value = false
                    saveState()
                    break
                }
                
                delay(500)
            }
        }
    }

    private fun saveState() {
        prefs.edit().apply {
            putLong("remaining_seconds", _remainingSeconds.value)
            putBoolean("is_running", _isRunning.value)
            putString("timer_mode", _timerMode.value.name)
            putInt("focus_duration", _focusDurationMinutes.value)
            putInt("break_duration", _breakDurationMinutes.value)
            putInt("total_sessions", _totalSessions.value)
            putInt("completed_sessions", _completedSessions.value)
            putString("current_purpose", _currentPurpose.value.name)
            putLong("target_end_time", targetEndTimeMillis)
            // elapsedRealtime은 재부팅 시 초기화되므로, 현재 시점의 elapsedRealtime도 같이 저장하여 보정 가능하게 함
            putLong("last_save_elapsed", SystemClock.elapsedRealtime())
            apply()
        }
    }

    private fun loadState() {
        _remainingSeconds.value = prefs.getLong("remaining_seconds", 0L)
        _isRunning.value = prefs.getBoolean("is_running", false)
        
        val savedMode = prefs.getString("timer_mode", TimerMode.NONE.name)
        _timerMode.value = TimerMode.fromName(savedMode)
        
        _focusDurationMinutes.value = prefs.getInt("focus_duration", 25)
        _breakDurationMinutes.value = prefs.getInt("break_duration", 5)
        _totalSessions.value = prefs.getInt("total_sessions", 1)
        _completedSessions.value = prefs.getInt("completed_sessions", 0)
        
        val savedPurpose = prefs.getString("current_purpose", PomodoroPurpose.OTHERS.name)
        _currentPurpose.value = PomodoroPurpose.fromName(savedPurpose)
        
        val savedTarget = prefs.getLong("target_end_time", 0L)
        val lastSaveElapsed = prefs.getLong("last_save_elapsed", 0L)
        val currentElapsed = SystemClock.elapsedRealtime()
        
        if (_isRunning.value && savedTarget > 0) {
            // 보정: 만약 시스템이 꺼져있지 않았다면 (currentElapsed >= lastSaveElapsed)
            // 남은 시간을 정확하게 다시 계산
            if (currentElapsed >= lastSaveElapsed) {
                targetEndTimeMillis = savedTarget
                val remaining = ((targetEndTimeMillis - currentElapsed + 999) / 1000).coerceAtLeast(0)
                _remainingSeconds.value = remaining
                if (remaining > 0) {
                    startTimerJob()
                } else {
                    _isRunning.value = false
                    saveState()
                }
            } else {
                // 재부팅된 경우 (elapsedRealtime이 리셋됨)
                // 저장된 remainingSeconds를 기반으로 새로 시작
                targetEndTimeMillis = currentElapsed + (_remainingSeconds.value * 1000)
                if (_remainingSeconds.value > 0) {
                    startTimerJob()
                } else {
                    _isRunning.value = false
                    saveState()
                }
            }
        }
    }
}

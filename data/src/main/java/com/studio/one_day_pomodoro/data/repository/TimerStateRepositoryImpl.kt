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

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

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
        saveState() // 즉시 저장
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
        saveState() // 즉시 저장
    }

    override fun resume() {
        if (_remainingSeconds.value > 0) {
            _isRunning.value = true
            targetEndTimeMillis = SystemClock.elapsedRealtime() + (_remainingSeconds.value * 1000)
            saveState() // 즉시 저장
            startTimerJob()
        }
    }

    override fun stop() {
        _isRunning.value = false
        _timerMode.value = TimerMode.NONE
        timerJob?.cancel()
        _remainingSeconds.value = 0
        saveState() // 즉시 저장
    }

    override fun clearExpiredState() {
        _completedSessions.value = 0
        _remainingSeconds.value = 0
        _timerMode.value = TimerMode.NONE
        _isRunning.value = false
        // preferences에서도 물리적으로 초기화
        prefs.edit().apply {
            putInt("completed_sessions", 0)
            putString("timer_mode", TimerMode.NONE.name)
            putBoolean("is_running", false)
            putLong("remaining_seconds", 0L)
            apply()
        }
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
        val savedIsRunning = prefs.getBoolean("is_running", false)
        if (!savedIsRunning) {
            _remainingSeconds.value = prefs.getLong("remaining_seconds", 0L)
            _isRunning.value = false
            _timerMode.value = TimerMode.fromName(prefs.getString("timer_mode", TimerMode.NONE.name))
            _focusDurationMinutes.value = prefs.getInt("focus_duration", 25)
            _breakDurationMinutes.value = prefs.getInt("break_duration", 5)
            _totalSessions.value = prefs.getInt("total_sessions", 1)
            _completedSessions.value = prefs.getInt("completed_sessions", 0)
            _currentPurpose.value = PomodoroPurpose.fromName(prefs.getString("current_purpose", PomodoroPurpose.OTHERS.name))
            _isInitialized.value = true
            return
        }

        // 실행 중이었던 경우, 현재 시각 기준으로 상태 시뮬레이션
        var currentMode = TimerMode.fromName(prefs.getString("timer_mode", TimerMode.NONE.name))
        var currentRemaining = prefs.getLong("remaining_seconds", 0L)
        var currentCompleted = prefs.getInt("completed_sessions", 0)
        val total = prefs.getInt("total_sessions", 1)
        val focusMin = prefs.getInt("focus_duration", 25)
        val breakMin = prefs.getInt("break_duration", 5)
        val purpose = PomodoroPurpose.fromName(prefs.getString("current_purpose", PomodoroPurpose.OTHERS.name))

        val savedTarget = prefs.getLong("target_end_time", 0L)
        val lastSaveElapsed = prefs.getLong("last_save_elapsed", 0L)
        val currentElapsed = SystemClock.elapsedRealtime()

        // 시간 보정: 저장 시점과 현재 시점 사이의 차이 계산
        val timePassedSinceSave = (currentElapsed - lastSaveElapsed).coerceAtLeast(0)
        
        // 현재 세션의 남은 시간 계산
        var targetEndTime = savedTarget
        var remainingInCurrentSession = (targetEndTime - currentElapsed + 999) / 1000

        // 만약 현재 세션이 이미 종료되었다면, 다음 세션으로 넘어가며 계산
        while (remainingInCurrentSession <= 0) {
            if (currentMode == TimerMode.FOCUS) {
                currentCompleted++
                if (currentCompleted >= total) {
                    // 모든 세션 종료
                    currentMode = TimerMode.NONE
                    remainingInCurrentSession = 0
                    break
                } else {
                    // 휴식으로 전환
                    currentMode = TimerMode.BREAK
                    val breakSec = breakMin * 60L
                    targetEndTime += (breakSec * 1000)
                    remainingInCurrentSession = (targetEndTime - currentElapsed + 999) / 1000
                }
            } else if (currentMode == TimerMode.BREAK) {
                // 다음 집중으로 전환
                currentMode = TimerMode.FOCUS
                val focusSec = focusMin * 60L
                targetEndTime += (focusSec * 1000)
                remainingInCurrentSession = (targetEndTime - currentElapsed + 999) / 1000
            } else {
                break
            }
        }

        // 최종 상태 반영
        _timerMode.value = currentMode
        _focusDurationMinutes.value = focusMin
        _breakDurationMinutes.value = breakMin
        _totalSessions.value = total
        _completedSessions.value = currentCompleted
        _currentPurpose.value = purpose

        if (currentMode != TimerMode.NONE && remainingInCurrentSession > 0) {
            _remainingSeconds.value = remainingInCurrentSession.coerceAtLeast(0)
            _isRunning.value = true
            targetEndTimeMillis = targetEndTime
            saveState() // 보정된 상태 저장
            startTimerJob()
        } else {
            _remainingSeconds.value = 0
            _isRunning.value = false
            saveState()
        }
        _isInitialized.value = true
    }
}

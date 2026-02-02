package com.studio.one_day_pomodoro.presentation.ui.screens.pomo_break

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.usecase.GetSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BreakViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val timerRepository: com.studio.one_day_pomodoro.domain.repository.TimerStateRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    // Repository 상태를 UI 상태로 변환
    val remainingSeconds: StateFlow<Long> = timerRepository.remainingSeconds
    
    private val _totalBreakSeconds = MutableStateFlow(0L)
    val totalBreakSeconds: StateFlow<Long> = _totalBreakSeconds.asStateFlow()
    
    private val _remainingRepeatCount = MutableStateFlow(0)
    val remainingRepeatCount: StateFlow<Int> = _remainingRepeatCount.asStateFlow()
    
    private val _settings = MutableStateFlow<com.studio.one_day_pomodoro.domain.model.PomodoroSettings?>(null)
    val settings: StateFlow<com.studio.one_day_pomodoro.domain.model.PomodoroSettings?> = _settings.asStateFlow()

    fun startBreak() {
        // 이미 휴식 중이면 다시 시작하지 않음 (알림에서 들어왔을 때 리셋 방지)
        if (timerRepository.isRunning.value && timerRepository.timerMode.value == com.studio.one_day_pomodoro.domain.model.TimerMode.BREAK) {
            _totalBreakSeconds.value = timerRepository.breakDurationMinutes.value * 60L
            return
        }

        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            _settings.value = settings
            _totalBreakSeconds.value = settings.breakMinutes * 60L
            
            // TimerStateRepository를 통해 타이머 시작 (BREAK 모드)
            timerRepository.start(
                seconds = _totalBreakSeconds.value,
                mode = com.studio.one_day_pomodoro.domain.model.TimerMode.BREAK,
                focusMin = settings.focusMinutes,
                breakMin = settings.breakMinutes,
                total = timerRepository.totalSessions.value,
                completed = timerRepository.completedSessions.value,
                purpose = timerRepository.currentPurpose.value
            )
            
            startTimerService(settings.breakMinutes)
        }
    }
    
    private fun startTimerService(durationMinutes: Int) {
        val intent = android.content.Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
            putExtra("DURATION_MINUTES", timerRepository.focusDurationMinutes.value)
            putExtra("BREAK_DURATION_MINUTES", durationMinutes)
            putExtra("TOTAL_SESSIONS", timerRepository.totalSessions.value)
            putExtra("COMPLETED_SESSIONS", timerRepository.completedSessions.value)
            putExtra("TIMER_MODE", com.studio.one_day_pomodoro.domain.model.TimerMode.BREAK.name)
            putExtra("PURPOSE", timerRepository.currentPurpose.value.name)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopBreak() {
        timerRepository.stop()
        stopTimerService()
    }
    
    private fun stopTimerService() {
        val intent = android.content.Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
        }
        context.stopService(intent)
    }
    
    fun setSessionInfo(remainingCount: Int) {
        _remainingRepeatCount.value = remainingCount
    }
}

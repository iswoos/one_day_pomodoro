package com.studio.one_day_pomodoro.presentation.ui.screens.timer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import com.studio.one_day_pomodoro.domain.model.TimerMode
import com.studio.one_day_pomodoro.domain.repository.TimerStateRepository
import com.studio.one_day_pomodoro.domain.usecase.GetSettingsUseCase
import com.studio.one_day_pomodoro.domain.usecase.SavePomodoroSessionUseCase
import com.studio.one_day_pomodoro.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject


@HiltViewModel
class TimerViewModel @Inject constructor(
    private val savePomodoroSessionUseCase: SavePomodoroSessionUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val timerRepository: TimerStateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Repository 상태를 UI 상태로 변환
    val remainingTimeSeconds: StateFlow<Long> = timerRepository.remainingSeconds
    val isTimerRunning: StateFlow<Boolean> = timerRepository.isRunning
    val timerMode: StateFlow<TimerMode> = timerRepository.timerMode
    val completedSessions: StateFlow<Int> = timerRepository.completedSessions
    val totalSessions: StateFlow<Int> = timerRepository.totalSessions
    val focusDurationMinutes: StateFlow<Int> = timerRepository.focusDurationMinutes

    private val _timerEvent = MutableSharedFlow<TimerEvent>()
    val timerEvent: SharedFlow<TimerEvent> = _timerEvent.asSharedFlow()

    private val _settings = MutableStateFlow<PomodoroSettings?>(null)
    val settings: StateFlow<PomodoroSettings?> = _settings.asStateFlow()

    private var startFocusMinutes: Int = 25

    init {
        loadSettings()
        observeTimerStateChanges()
    }
    
    // TimerService가 처리하는 상태 변경을 감지하여 UI/Logic 처리
    private fun observeTimerStateChanges() {
        // ViewModel no longer manages transitions. It only emits events for UI navigation.
        viewModelScope.launch {
            timerRepository.timerMode.collect { mode ->
                if (mode == TimerMode.BREAK) {
                    _timerEvent.emit(TimerEvent.GoToBreak(timerRepository.focusDurationMinutes.value))
                }
            }
        }
        
        viewModelScope.launch {
            timerRepository.isRunning.collect { isRunning ->
                if (!isRunning) {
                     val seconds = timerRepository.remainingSeconds.value
                     if (seconds == 0L) {
                         val completed = timerRepository.completedSessions.value
                         val total = timerRepository.totalSessions.value
                         // 모든 세션이 정말 끝났을 때 (모드가 이미 NONE으로 바뀌었을 수 있으므로 세션 수로 판단)
                         if (completed > 0 && completed >= total) {
                             _timerEvent.emit(TimerEvent.Finished(
                                 timerRepository.currentPurpose.value, 
                                 completed * timerRepository.focusDurationMinutes.value
                             ))
                         }
                     }
                }
            }
        }
    }
    
    private fun onFocusSessionFinished() {
        // 더 이상 ViewModel에서 별도의 상태(_remainingRepeatCount 등)를 관리하지 않음
        // Service가 Repository를 갱신하면 UI는 이를 관찰하여 보여줌
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect {
                _settings.value = it
            }
        }
    }

    fun startTimer(purpose: PomodoroPurpose) {
        // 이미 실행 중(FOCUS든 BREAK든)이면 무시하여 리셋 방지
        val running = timerRepository.isRunning.value
        if (running) return
        
        viewModelScope.launch {
            val settings = _settings.value ?: getSettingsUseCase().first()
            startFocusMinutes = settings.focusMinutes
            
            val total = settings.repeatCount
            val completed = 0 
            
            // ViewModel은 서비스에 '시작해라'라고 요청만 함.
            // 서비스가 실제 Repository.start()를 호출하여 초기화함.
            startTimerService(startFocusMinutes, settings.breakMinutes, total, completed, purpose, TimerMode.FOCUS)
        }
    }

    fun setTimer(purpose: PomodoroPurpose) {
        val duration = _settings.value?.focusMinutes ?: 25
        startFocusMinutes = duration
    }

    fun toggleTimer() {
        val isRunning = timerRepository.isRunning.value
        if (isRunning) {
            timerRepository.pause()
            stopTimerService() 
        } else {
            val currentSeconds = timerRepository.remainingSeconds.value
            val settings = _settings.value 
            val breakMinutes = settings?.breakMinutes ?: 5
            
            if (currentSeconds > 0) {
                 timerRepository.resume()
                 startTimerService(
                     timerRepository.focusDurationMinutes.value, 
                     timerRepository.breakDurationMinutes.value,
                     timerRepository.totalSessions.value,
                     timerRepository.completedSessions.value,
                     timerRepository.currentPurpose.value,
                     timerRepository.timerMode.value
                 ) 
            } else {
                // 이 상황은 보통 발생하지 않지만 (끝나면 stop됨), 안전장치
                val focusMin = settings?.focusMinutes ?: 25
                // Repository start는 Service에서 할 것이므로 여기서는 Service만 호출
                startTimerService(
                    focusMin, 
                    breakMinutes, 
                    settings?.repeatCount ?: 1, 
                    0, 
                    timerRepository.currentPurpose.value,
                    TimerMode.FOCUS
                )
            }
        }
    }

    fun stopTimer(createEvent: Boolean = true) {
        val purpose = timerRepository.currentPurpose.value
        val minutesSpent = timerRepository.completedSessions.value * timerRepository.focusDurationMinutes.value
        
        timerRepository.stop()
        stopTimerService()
        if (createEvent) {
            viewModelScope.launch {
                _timerEvent.emit(TimerEvent.Finished(purpose, minutesSpent))
            }
        }
    }

    private fun startTimerService(durationMinutes: Int, breakMinutes: Int, totalSessions: Int, completedSessions: Int, purpose: PomodoroPurpose, mode: TimerMode) {
        val intent = Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
            putExtra("DURATION_MINUTES", durationMinutes)
            putExtra("BREAK_DURATION_MINUTES", breakMinutes)
            putExtra("TOTAL_SESSIONS", totalSessions)
            putExtra("COMPLETED_SESSIONS", completedSessions)
            putExtra("TIMER_MODE", mode.name)
            putExtra("PURPOSE", purpose.name)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopTimerService() {
        val intent = Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
        }
        context.stopService(intent)
    }

    sealed interface TimerEvent {
        data class GoToBreak(val minutes: Int) : TimerEvent
        data class Finished(val purpose: PomodoroPurpose, val minutes: Int) : TimerEvent
    }
}

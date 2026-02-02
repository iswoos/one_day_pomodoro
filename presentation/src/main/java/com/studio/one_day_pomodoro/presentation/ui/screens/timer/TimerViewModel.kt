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

    private val _totalFocusTimeSeconds = MutableStateFlow(1L)
    val totalFocusTimeSeconds: StateFlow<Long> = _totalFocusTimeSeconds.asStateFlow()

    private val _timerEvent = MutableSharedFlow<TimerEvent>()
    val timerEvent: SharedFlow<TimerEvent> = _timerEvent.asSharedFlow()

    private val _settings = MutableStateFlow<PomodoroSettings?>(null)
    val settings: StateFlow<PomodoroSettings?> = _settings.asStateFlow()

    private val _remainingRepeatCount = MutableStateFlow(0)
    val remainingRepeatCount: StateFlow<Int> = _remainingRepeatCount.asStateFlow()

    private var currentPurpose: PomodoroPurpose? = null
    private var startFocusMinutes: Int = 25
    private var currentAccumulatedMinutes = 0

    init {
        loadSettings()
        observeTimerStateChanges()
    }
    
    // TimerService가 처리하는 상태 변경을 감지하여 UI/Logic 처리
    private fun observeTimerStateChanges() {
        // 1. 모드가 BREAK로 변경됨 (Auto-Transition: Focus -> Break)
        viewModelScope.launch {
            timerRepository.timerMode.collect { mode ->
                if (mode == TimerMode.BREAK) {
                    onFocusSessionFinished()
                    _timerEvent.emit(TimerEvent.GoToBreak(timerRepository.focusDurationMinutes.value))
                }
            }
        }
        
        // 2. 타이머가 멈춤 (Last Session Finished or Manual Stop)
        viewModelScope.launch {
            timerRepository.isRunning.collect { isRunning ->
                if (!isRunning) {
                     val seconds = timerRepository.remainingSeconds.value
                     
                     // 0초이고, 모드가 FOCUS일 때만 '완료' 이벤트 발생
                     // Break가 0초가 되어서 멈추는 경우는 없음 (Auto-Loop로 Focus로 넘어가거나, 진짜 끝났으면 isLastCheck)
                     // Service에서 Stop을 호출하는 경우는:
                     // 1. User Stop -> seconds > 0
                     // 2. Last Focus Finished -> seconds == 0
                     // 3. Break Finished but something wrong (should not happen in loop)
                     
                     if (seconds == 0L && currentPurpose != null) {
                         // 중요: Break 모드에서 0초가 된 후 Focus로 넘어가는 찰나에 이 로직이 돌면 안됨.
                         // 따라서 현재 모드가 FOCUS인지 확인해야 함.
                         val currentMode = timerRepository.timerMode.value
                         if (currentMode == TimerMode.FOCUS) {
                             onFocusSessionFinished()
                             _timerEvent.emit(TimerEvent.Finished(currentPurpose ?: PomodoroPurpose.OTHERS, currentAccumulatedMinutes))
                             currentPurpose = null
                         }
                     }
                }
            }
        }
    }
    
    private fun onFocusSessionFinished() {
        currentAccumulatedMinutes += startFocusMinutes
        if (_remainingRepeatCount.value > 0) { // 1보다 클 때만 줄이는게 아니라, 완료했으니 무조건 줄임 (단 0 이상일 때)
            _remainingRepeatCount.value -= 1
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect {
                _settings.value = it
                if (_remainingRepeatCount.value == 0) {
                     _remainingRepeatCount.value = it.repeatCount
                }
            }
        }
    }

    fun startTimer(purpose: PomodoroPurpose) {
        // 이미 실행 중이고 같은 모드(FOCUS)면 무시하여 리셋 방지
        if (timerRepository.isRunning.value && timerRepository.timerMode.value == TimerMode.FOCUS) return
        
        viewModelScope.launch {
            val settings = _settings.value ?: getSettingsUseCase().first()
            startFocusMinutes = settings.focusMinutes
            _totalFocusTimeSeconds.value = startFocusMinutes * 60L
            
            val total = settings.repeatCount
            val completed = 0 // 처음 시작은 0
            
            timerRepository.start(
                seconds = startFocusMinutes * 60L,
                mode = TimerMode.FOCUS,
                focusMin = startFocusMinutes,
                breakMin = settings.breakMinutes,
                total = total,
                completed = completed,
                purpose = purpose
            )
            val isLast = total <= 1
            startTimerService(startFocusMinutes, settings.breakMinutes, total, completed, purpose)
        }
    }

    fun setTimer(purpose: PomodoroPurpose) {
        currentPurpose = purpose
        val duration = _settings.value?.focusMinutes ?: 25
        startFocusMinutes = duration
        _totalFocusTimeSeconds.value = duration * 60L
        
        if (_remainingRepeatCount.value == 0) {
            _remainingRepeatCount.value = _settings.value?.repeatCount ?: 4
        }
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
                     timerRepository.currentPurpose.value
                 ) 
            } else {
                // 이 상황은 보통 발생하지 않지만 (끝나면 stop됨), 안전장치
                val focusMin = settings?.focusMinutes ?: 25
                timerRepository.start(
                    seconds = focusMin * 60L,
                    mode = TimerMode.FOCUS,
                    focusMin = focusMin,
                    breakMin = breakMinutes,
                    total = settings?.repeatCount ?: 1,
                    completed = 0,
                    purpose = currentPurpose ?: PomodoroPurpose.OTHERS
                )
                startTimerService(
                    focusMin, 
                    breakMinutes, 
                    settings?.repeatCount ?: 1, 
                    0, 
                    currentPurpose ?: PomodoroPurpose.OTHERS
                )
            }
        }
    }

    fun stopTimer(createEvent: Boolean = true) {
        timerRepository.stop()
        stopTimerService()
        if (createEvent) {
            viewModelScope.launch {
                _timerEvent.emit(TimerEvent.Finished(currentPurpose ?: PomodoroPurpose.OTHERS, currentAccumulatedMinutes))
                currentPurpose = null // Stop 시 초기화
            }
        }
    }

    private fun startTimerService(durationMinutes: Int, breakMinutes: Int, totalSessions: Int, completedSessions: Int, purpose: PomodoroPurpose) {
        val intent = Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
            putExtra("DURATION_MINUTES", durationMinutes)
            putExtra("BREAK_DURATION_MINUTES", breakMinutes)
            putExtra("TOTAL_SESSIONS", totalSessions)
            putExtra("COMPLETED_SESSIONS", completedSessions)
            putExtra("TIMER_MODE", timerRepository.timerMode.value.name)
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

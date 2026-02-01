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
        // 1. 모드가 BREAK로 변경됨 (Auto-Transition)
        viewModelScope.launch {
            timerRepository.timerMode.collect { mode ->
                if (mode == TimerMode.BREAK) {
                    // Service가 이미 저장을 완료하고 모드를 변경함
                    if (currentPurpose != null) {
                        onFocusSessionFinished()
                        _timerEvent.emit(TimerEvent.GoToBreak(startFocusMinutes))
                        currentPurpose = null // Break 모드 진입 시 목적 초기화 (표시용)
                    }
                }
            }
        }
        
        // 2. 타이머가 멈춤 (Last Session Finished or Manual Stop)
        viewModelScope.launch {
            timerRepository.isRunning.collect { isRunning ->
                if (!isRunning) {
                     val seconds = timerRepository.remainingSeconds.value
                     if (seconds == 0L && currentPurpose != null) {
                         // 마지막 세션 완료 (Service가 Stop 처리함)
                         onFocusSessionFinished()
                         _timerEvent.emit(TimerEvent.Finished(currentPurpose ?: PomodoroPurpose.OTHERS, currentAccumulatedMinutes))
                         currentPurpose = null
                     }
                }
            }
        }
    }
    
    private fun onFocusSessionFinished() {
        currentAccumulatedMinutes += startFocusMinutes
        if (_remainingRepeatCount.value > 1) {
            _remainingRepeatCount.value -= 1
        }
        // 저장은 Service가 수행했으므로 여기서는 생략
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
        // 이미 실행 중이고 같은 목적이면 무시
        if (currentPurpose == purpose && isTimerRunning.value) return
        
        val isFirstStart = currentPurpose == null
        currentPurpose = purpose

        viewModelScope.launch {
            if (isFirstStart) {
                // 설정값을 확실히 가져옴
                val settings = _settings.value ?: getSettingsUseCase().first()
                startFocusMinutes = settings.focusMinutes
                _totalFocusTimeSeconds.value = startFocusMinutes * 60L
                
                // 처음 시작할 때 Repository에 시간 설정 & 시작
                timerRepository.start(startFocusMinutes * 60L, TimerMode.FOCUS)
                val isLast = _remainingRepeatCount.value <= 1
                startTimerService(startFocusMinutes, settings.breakMinutes, isLast, purpose)
            } else {
                // 재개
                timerRepository.resume()
                val settings = _settings.value ?: getSettingsUseCase().first()
                val isLast = _remainingRepeatCount.value <= 1
                startTimerService(startFocusMinutes, settings.breakMinutes, isLast, purpose)
            }
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
            val isLast = _remainingRepeatCount.value <= 1
            val settings = _settings.value 
            val breakMinutes = settings?.breakMinutes ?: 5
            
            if (currentSeconds > 0) {
                 timerRepository.resume()
                 startTimerService(startFocusMinutes, breakMinutes, isLast, currentPurpose ?: PomodoroPurpose.OTHERS) 
            } else {
                timerRepository.start(startFocusMinutes * 60L, TimerMode.FOCUS)
                startTimerService(startFocusMinutes, breakMinutes, isLast, currentPurpose ?: PomodoroPurpose.OTHERS)
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

    private fun startTimerService(durationMinutes: Int, breakMinutes: Int, isLastSession: Boolean, purpose: PomodoroPurpose) {
        val intent = Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
            putExtra("DURATION_MINUTES", durationMinutes)
            putExtra("BREAK_DURATION_MINUTES", breakMinutes)
            putExtra("IS_LAST_SESSION", isLastSession)
            putExtra("TIMER_MODE", TimerMode.FOCUS.name)
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

    // completeSession 제거됨 (Service 위임)

    sealed interface TimerEvent {
        data class GoToBreak(val minutes: Int) : TimerEvent
        data class Finished(val purpose: PomodoroPurpose, val minutes: Int) : TimerEvent
    }
}

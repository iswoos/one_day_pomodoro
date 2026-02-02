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
                    if (currentPurpose != null) {
                         // Focus 완료 처리는 Service가 했음. UI 상태만 업데이트.
                         // 단, onFocusSessionFinished()는 호출해줘야 다음 카운트로 넘어감.
                         onFocusSessionFinished()
                         _timerEvent.emit(TimerEvent.GoToBreak(startFocusMinutes))
                         // currentPurpose는 유지해야 다음 Focus 때 사용 가능하지만,
                         // 여기서는 break 끝나고 다시 focus로 갈 때 다시 세팅되어야 함?
                         // 아니면 루프를 위해 유지? -> 유지해야 함.
                         // 하지만 BreakScreen에서는 purpose가 필요 없음.
                         // Focus로 돌아올 때 purpose가 null이면 곤란함.
                         // 따라서 null 처리 하지 않음.
                    }
                } else if (mode == TimerMode.FOCUS) {
                    // Break -> Focus (Auto-Loop) 감지
                    // 만약 서비스가 루프를 돌려서 Focus로 왔다면...
                    // 여기서 isLastSession 정보를 다시 서비스에 업데이트 해줘야 함.
                    if (currentPurpose != null && _remainingRepeatCount.value > 0) {
                        // 이미 실행 중인 서비스에 업데이트 Intent 전송
                        val isLast = _remainingRepeatCount.value <= 1 // 이번이 마지막임
                        val settings = _settings.value
                        if (settings != null) {
                            startTimerService(settings.focusMinutes, settings.breakMinutes, isLast, currentPurpose!!)
                        }
                    }
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
        // 이미 실행 중이고 같은 목적이면 무시
        if (currentPurpose == purpose && isTimerRunning.value) return
        
        val isFirstStart = currentPurpose == null
        currentPurpose = purpose

        viewModelScope.launch {
            if (isFirstStart) {
                val settings = _settings.value ?: getSettingsUseCase().first()
                startFocusMinutes = settings.focusMinutes
                _totalFocusTimeSeconds.value = startFocusMinutes * 60L
                
                timerRepository.start(startFocusMinutes * 60L, TimerMode.FOCUS)
                val isLast = _remainingRepeatCount.value <= 1
                startTimerService(startFocusMinutes, settings.breakMinutes, isLast, purpose)
            } else {
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
            putExtra("TIMER_MODE", TimerMode.FOCUS.name) // 일단 호출할 때는 Focus라고 가정하나, 루프 중 업데이트일 수 있음.
            // 하지만 이 함수는 startFocusMinutes를 받으므로 Focus 시작/재개 용도임.
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

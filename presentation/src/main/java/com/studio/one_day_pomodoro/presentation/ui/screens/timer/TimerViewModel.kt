package com.studio.one_day_pomodoro.presentation.ui.screens.timer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
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
        observeTimerCompletion()
    }
    
    // 타이머가 0이 되었을 때(완료) 감지
    private fun observeTimerCompletion() {
        viewModelScope.launch {
            timerRepository.remainingSeconds.collect { seconds ->
                if (seconds == 0L && _settings.value != null && timerRepository.isRunning.value == false) {
                     if (currentPurpose != null) {
                         completeSession()
                         currentPurpose = null 
                     }
                }
            }
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
                // 설정값을 확실히 가져옴
                val settings = _settings.value ?: getSettingsUseCase().first()
                startFocusMinutes = settings.focusMinutes
                _totalFocusTimeSeconds.value = startFocusMinutes * 60L
                
                // 처음 시작할 때 Repository에 시간 설정 & 시작
                timerRepository.start(startFocusMinutes * 60L)
                startTimerService(startFocusMinutes)
            } else {
                // 재개
                timerRepository.resume()
                startTimerService(startFocusMinutes)
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
            if (currentSeconds > 0) {
                 timerRepository.resume()
                 startTimerService(startFocusMinutes) 
            } else {
                timerRepository.start(startFocusMinutes * 60L)
                startTimerService(startFocusMinutes)
            }
        }
    }

    fun stopTimer() {
        timerRepository.stop()
        stopTimerService()
        viewModelScope.launch {
            _timerEvent.emit(TimerEvent.Finished(currentPurpose ?: PomodoroPurpose.OTHERS, currentAccumulatedMinutes))
        }
    }

    private fun startTimerService(durationMinutes: Int) {
        val intent = Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
            putExtra("DURATION_MINUTES", durationMinutes)
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

    private fun completeSession() {
        stopTimerService()
        val purpose = currentPurpose ?: PomodoroPurpose.OTHERS
        
        viewModelScope.launch {
            savePomodoroSessionUseCase(
                PomodoroSession(
                    purpose = purpose,
                    focusDurationInMinutes = startFocusMinutes,
                    completedAt = LocalDateTime.now()
                )
            )
            
            currentAccumulatedMinutes += startFocusMinutes
            
            if (_remainingRepeatCount.value > 1) {
                _remainingRepeatCount.value -= 1
                _timerEvent.emit(TimerEvent.GoToBreak(startFocusMinutes))
            } else {
                _timerEvent.emit(TimerEvent.Finished(purpose, currentAccumulatedMinutes))
            }
        }
    }

    sealed interface TimerEvent {
        data class GoToBreak(val minutes: Int) : TimerEvent
        data class Finished(val purpose: PomodoroPurpose, val minutes: Int) : TimerEvent
    }
}


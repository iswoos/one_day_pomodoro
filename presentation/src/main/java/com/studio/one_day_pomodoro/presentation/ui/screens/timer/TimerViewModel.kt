package com.studio.one_day_pomodoro.presentation.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import com.studio.one_day_pomodoro.domain.usecase.GetSettingsUseCase
import com.studio.one_day_pomodoro.domain.usecase.SavePomodoroSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getSettingsUseCase: GetSettingsUseCase,
    private val savePomodoroSessionUseCase: SavePomodoroSessionUseCase
) : ViewModel() {

    private val _remainingTimeSeconds = MutableStateFlow(0L)
    val remainingTimeSeconds: StateFlow<Long> = _remainingTimeSeconds.asStateFlow()

    private val _totalFocusTimeSeconds = MutableStateFlow(1L)
    val totalFocusTimeSeconds: StateFlow<Long> = _totalFocusTimeSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerEvent = MutableSharedFlow<TimerEvent>()
    val timerEvent: SharedFlow<TimerEvent> = _timerEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var currentPurpose: PomodoroPurpose? = null
    private var initialFocusMinutes: Int = 25
    private var totalRepeatCount: Int = 0
    private var remainingRepeatCount: Int = 0
    private var currentAccumulatedMinutes: Int = 0

    fun startTimer(purpose: PomodoroPurpose) {
        if (currentPurpose == purpose && _isTimerRunning.value) return
        
        val isFirstStart = currentPurpose == null
        currentPurpose = purpose
        
        viewModelScope.launch {
            if (isFirstStart) {
                val settings = getSettingsUseCase().first()
                initialFocusMinutes = settings.focusMinutes
                totalRepeatCount = settings.repeatCount
                remainingRepeatCount = totalRepeatCount
                currentAccumulatedMinutes = 0
                _totalFocusTimeSeconds.value = initialFocusMinutes * 60L
                _remainingTimeSeconds.value = initialFocusMinutes * 60L
            }
            
            _isTimerRunning.value = true
            runTimer()
        }
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingTimeSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _remainingTimeSeconds.value -= 1
            }
            if (_remainingTimeSeconds.value == 0L) {
                completeSession()
            }
        }
    }

    private suspend fun completeSession() {
        _isTimerRunning.value = false
        val purpose = currentPurpose ?: PomodoroPurpose.OTHERS
        
        savePomodoroSessionUseCase(
            PomodoroSession(
                purpose = purpose,
                focusDurationInMinutes = initialFocusMinutes,
                completedAt = LocalDateTime.now()
            )
        )
        
        currentAccumulatedMinutes += initialFocusMinutes
        
        if (remainingRepeatCount > 1) {
            remainingRepeatCount -= 1
            _remainingTimeSeconds.value = initialFocusMinutes * 60L
            _timerEvent.emit(TimerEvent.GoToBreak(initialFocusMinutes))
        } else {
            _timerEvent.emit(TimerEvent.Finished(purpose, currentAccumulatedMinutes))
        }
    }

    fun toggleTimer() {
        _isTimerRunning.value = !_isTimerRunning.value
        if (_isTimerRunning.value) {
            runTimer()
        } else {
            timerJob?.cancel()
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
        _remainingTimeSeconds.value = 0
        currentPurpose = null
    }

    sealed interface TimerEvent {
        data class GoToBreak(val minutes: Int) : TimerEvent
        data class Finished(val purpose: PomodoroPurpose, val minutes: Int) : TimerEvent
    }
}

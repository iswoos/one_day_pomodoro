package com.studio.one_day_pomodoro.presentation.ui.screens.pomo_break

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.usecase.GetSettingsUseCase
import com.studio.one_day_pomodoro.domain.model.TimerMode
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val timerMode: StateFlow<TimerMode> = timerRepository.timerMode
    val completedSessions: StateFlow<Int> = timerRepository.completedSessions
    val totalSessions: StateFlow<Int> = timerRepository.totalSessions
    val breakDurationMinutes: StateFlow<Int> = timerRepository.breakDurationMinutes
    
    private val _settings = MutableStateFlow<com.studio.one_day_pomodoro.domain.model.PomodoroSettings?>(null)
    val settings: StateFlow<com.studio.one_day_pomodoro.domain.model.PomodoroSettings?> = _settings.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect {
                _settings.value = it
            }
        }
    }

    fun startBreak() {
        // ViewModel no longer starts the break. 
        // Service handles the transition and repository update.
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
}

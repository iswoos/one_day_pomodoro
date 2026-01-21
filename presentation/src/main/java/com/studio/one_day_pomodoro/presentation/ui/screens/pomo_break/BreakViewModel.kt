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
    private val getSettingsUseCase: GetSettingsUseCase
) : ViewModel() {

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _totalBreakSeconds = MutableStateFlow(0L)
    val totalBreakSeconds: StateFlow<Long> = _totalBreakSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun startBreak() {
        viewModelScope.launch {
            val settings = getSettingsUseCase().first()
            _totalBreakSeconds.value = settings.breakMinutes * 60L
            _remainingSeconds.value = _totalBreakSeconds.value
            
            runTimer()
        }
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
        }
    }
}

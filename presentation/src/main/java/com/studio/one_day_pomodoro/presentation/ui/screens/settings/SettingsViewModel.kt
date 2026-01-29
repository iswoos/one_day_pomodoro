package com.studio.one_day_pomodoro.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import com.studio.one_day_pomodoro.domain.usecase.GetSettingsUseCase
import com.studio.one_day_pomodoro.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<PomodoroSettings?> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateFocusMinutes(delta: Int) {
        settings.value?.let { current ->
            val newValue = (current.focusMinutes + delta).coerceIn(1, 180)
            updateSettings(current.copy(focusMinutes = newValue))
        }
    }

    fun setFocusMinutes(value: String) {
        val intValue = value.toIntOrNull() ?: return
        settings.value?.let { current ->
            updateSettings(current.copy(focusMinutes = intValue.coerceIn(1, 180)))
        }
    }

    fun updateBreakMinutes(delta: Int) {
        settings.value?.let { current ->
            val newValue = (current.breakMinutes + delta).coerceIn(1, 60)
            updateSettings(current.copy(breakMinutes = newValue))
        }
    }

    fun setBreakMinutes(value: String) {
        val intValue = value.toIntOrNull() ?: return
        settings.value?.let { current ->
            updateSettings(current.copy(breakMinutes = intValue.coerceIn(1, 60)))
        }
    }

    fun updateRepeatCount(delta: Int) {
        settings.value?.let { current ->
            val newValue = (current.repeatCount + delta).coerceIn(1, 10)
            updateSettings(current.copy(repeatCount = newValue))
        }
    }

    fun setRepeatCount(value: String) {
        val intValue = value.toIntOrNull() ?: return
        settings.value?.let { current ->
            updateSettings(current.copy(repeatCount = intValue.coerceIn(1, 10)))
        }
    }

    private fun updateSettings(newSettings: PomodoroSettings) {
        viewModelScope.launch {
            updateSettingsUseCase(newSettings)
        }
    }
}

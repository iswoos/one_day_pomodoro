package com.studio.one_day_pomodoro.presentation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import com.studio.one_day_pomodoro.domain.usecase.GetSettingsUseCase
import com.studio.one_day_pomodoro.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _realSettings = getSettingsUseCase()
    
    // UI에서 보여줄 임시 설정값 (저장 전까지는 DB에 반영 안됨)
    private val _temporarySettings = MutableStateFlow<PomodoroSettings?>(null)
    val settings: StateFlow<PomodoroSettings?> = _temporarySettings.asStateFlow()

    init {
        viewModelScope.launch {
            _realSettings.collect { real ->
                // 초기 로드 시에만, 혹은 필요하다면 realSettings가 바뀔 때마다 동기화
                // 여기서는 "저장 안하고 나갔다 들어왔을 때" 초기화되어야 하므로
                // 화면 진입 시점(ViewModel 생성)에 한번 초기화하거나, 
                // 명시적으로 reset 기능을 두는 것이 좋음.
                // 일단 최초 로드 시 임시값도 초기화
                if (_temporarySettings.value == null) {
                    _temporarySettings.value = real
                }
            }
        }
    }

    // 화면 재진입 시 호출하여 DB값으로 리셋 (뒤로가기 했다가 다시 들어왔을 때 반영 안되게 하기 위함)
    fun refreshSettings() {
        viewModelScope.launch {
             // 현재 DB값(Flow)에서 최신 값을 가져와서 임시값에 덮어씌움
             // _realSettings는 Flow이므로 collector가 필요하지만, 
             // stateIn으로 변환하여 value를 가져오거나 별도 job으로 처리
             // 간단하게는 GetSettingsUseCase를 한 번 더 호출하거나, collect 구조 활용
             // 여기서는 간단히 null로 두어 init 블록 로직이나, 아래 collect 로직이 다시 돌게 유도할 수도 있으나,
             // 이미 collect 중이므로, 별도 변수 없이도 동작할 수 있도록 구조를 잡아야 함.
        }
    }
    
    // 하지만 Jetpack Compose Navigation에서는 ViewModel이 화면 스코프에 따라 유지될 수 있음.
    // "뒤로가기"를 하면 화면이 pop되어 ViewModel이 cleared 됨 -> 다시 들어오면 새로 생성 -> init 블록 실행 -> DB값 로드 -> OK.
    // 즉, 별도 refresh 호출 없이 ViewModel이 재생성된다면 문제 없음.
    // 만약 Hilt Navigation graph scope가 상위라면 유효하지 않을 수 있음.
    // 안전을 위해 init 블록 로직을 믿되, update 메서드들이 _temporarySettings만 건드리는지 확인.

    fun updateFocusMinutes(delta: Int) {
        _temporarySettings.value?.let { current ->
            val newValue = (current.focusMinutes + delta).coerceIn(1, 180)
            _temporarySettings.value = current.copy(focusMinutes = newValue)
        }
    }

    fun setFocusMinutes(value: String) {
        val intValue = value.toIntOrNull() ?: return
        _temporarySettings.value?.let { current ->
            _temporarySettings.value = current.copy(focusMinutes = intValue.coerceIn(1, 180))
        }
    }

    fun updateBreakMinutes(delta: Int) {
        _temporarySettings.value?.let { current ->
            val newValue = (current.breakMinutes + delta).coerceIn(1, 60)
            _temporarySettings.value = current.copy(breakMinutes = newValue)
        }
    }

    fun setBreakMinutes(value: String) {
        val intValue = value.toIntOrNull() ?: return
        _temporarySettings.value?.let { current ->
            _temporarySettings.value = current.copy(breakMinutes = intValue.coerceIn(1, 60))
        }
    }

    fun updateRepeatCount(delta: Int) {
        _temporarySettings.value?.let { current ->
            val newValue = (current.repeatCount + delta).coerceIn(1, 10)
            _temporarySettings.value = current.copy(repeatCount = newValue)
        }
    }

    fun setRepeatCount(value: String) {
        val intValue = value.toIntOrNull() ?: return
        _temporarySettings.value?.let { current ->
            _temporarySettings.value = current.copy(repeatCount = intValue.coerceIn(1, 10))
        }
    }

    fun toggleVibrationEnabled(enabled: Boolean) {
        _temporarySettings.value?.let { current ->
            _temporarySettings.value = current.copy(vibrationEnabled = enabled)
        }
    }

    fun setVibrationIntensity(intensity: Float) {
        _temporarySettings.value?.let { current ->
            _temporarySettings.value = current.copy(vibrationIntensity = intensity)
        }
    }

    fun saveSettings() {
        _temporarySettings.value?.let { newSettings ->
            viewModelScope.launch {
                updateSettingsUseCase(newSettings)
            }
        }
    }
}

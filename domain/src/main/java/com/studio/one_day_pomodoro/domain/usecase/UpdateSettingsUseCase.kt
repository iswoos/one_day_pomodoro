package com.studio.one_day_pomodoro.domain.usecase

import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import com.studio.one_day_pomodoro.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * 뽀모도로 설정을 업데이트하는 유스케이스입니다.
 */
class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: PomodoroSettings) {
        repository.updateSettings(settings)
    }
}

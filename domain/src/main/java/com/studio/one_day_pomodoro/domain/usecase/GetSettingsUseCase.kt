package com.studio.one_day_pomodoro.domain.usecase

import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import com.studio.one_day_pomodoro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 현재 뽀모도로 설정을 가져오는 유스케이스입니다.
 */
class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<PomodoroSettings> {
        return repository.getSettings()
    }
}

package com.studio.one_day_pomodoro.domain.repository

import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import kotlinx.coroutines.flow.Flow

/**
 * 앱 설정을 관리하는 저장소 인터페이스입니다.
 */
interface SettingsRepository {
    /**
     * 현재 뽀모도로 설정을 가져옵니다.
     */
    fun getSettings(): Flow<PomodoroSettings>

    /**
     * 뽀모도로 설정을 업데이트합니다.
     */
    suspend fun updateSettings(settings: PomodoroSettings)
}

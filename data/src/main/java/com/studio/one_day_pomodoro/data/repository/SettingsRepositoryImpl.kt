package com.studio.one_day_pomodoro.data.repository

import com.studio.one_day_pomodoro.data.datastore.SettingsDataStore
import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import com.studio.one_day_pomodoro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<PomodoroSettings> {
        return settingsDataStore.settingsFlow
    }

    override suspend fun updateSettings(settings: PomodoroSettings) {
        settingsDataStore.updateSettings(settings)
    }
}

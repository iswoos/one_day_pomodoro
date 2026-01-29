package com.studio.one_day_pomodoro.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.studio.one_day_pomodoro.domain.model.PomodoroSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {
    private val focusMinutesKey = intPreferencesKey("focus_minutes")
    private val breakMinutesKey = intPreferencesKey("break_minutes")
    private val repeatCountKey = intPreferencesKey("repeat_count")

    val settingsFlow: Flow<PomodoroSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PomodoroSettings(
                focusMinutes = preferences[focusMinutesKey] ?: 25,
                breakMinutes = preferences[breakMinutesKey] ?: 5,
                repeatCount = preferences[repeatCountKey] ?: 0
            )
        }

    suspend fun updateSettings(settings: PomodoroSettings) {
        context.dataStore.edit { preferences ->
            preferences[focusMinutesKey] = settings.focusMinutes
            preferences[breakMinutesKey] = settings.breakMinutes
            preferences[repeatCountKey] = settings.repeatCount
        }
    }
}

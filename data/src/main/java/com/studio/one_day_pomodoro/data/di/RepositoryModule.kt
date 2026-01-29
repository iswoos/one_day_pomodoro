package com.studio.one_day_pomodoro.data.di

import com.studio.one_day_pomodoro.data.repository.PomodoroRepositoryImpl
import com.studio.one_day_pomodoro.data.repository.SettingsRepositoryImpl
import com.studio.one_day_pomodoro.data.repository.TimerStateRepositoryImpl
import com.studio.one_day_pomodoro.domain.repository.PomodoroRepository
import com.studio.one_day_pomodoro.domain.repository.SettingsRepository
import com.studio.one_day_pomodoro.domain.repository.TimerStateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPomodoroRepository(
        pomodoroRepositoryImpl: PomodoroRepositoryImpl
    ): PomodoroRepository

    @Binds
    @Singleton
    abstract fun bindTimerStateRepository(
        timerStateRepositoryImpl: TimerStateRepositoryImpl
    ): TimerStateRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}

package com.studio.one_day_pomodoro.data.di

import android.content.Context
import androidx.room.Room
import com.studio.one_day_pomodoro.data.database.PomodoroDao
import com.studio.one_day_pomodoro.data.database.PomodoroDatabase
import com.studio.one_day_pomodoro.data.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun providePomodoroDatabase(
        @ApplicationContext context: Context
    ): PomodoroDatabase {
        return Room.databaseBuilder(
            context,
            PomodoroDatabase::class.java,
            "pomodoro_db"
        ).build()
    }

    @Provides
    fun providePomodoroDao(database: PomodoroDatabase): PomodoroDao {
        return database.pomodoroDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): android.content.SharedPreferences {
        return context.getSharedPreferences("pomo_timer_prefs", Context.MODE_PRIVATE)
    }
}

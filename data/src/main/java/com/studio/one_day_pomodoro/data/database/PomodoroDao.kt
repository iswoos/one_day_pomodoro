package com.studio.one_day_pomodoro.data.database

import androidx.room.*
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import java.time.LocalDateTime

/**
 * Room Database에서 사용할 세션 엔티티입니다.
 */
@Entity(tableName = "pomodoro_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purpose: PomodoroPurpose,
    val focusDurationInMinutes: Int,
    val completedAt: LocalDateTime
)

@Dao
interface PomodoroDao {
    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM pomodoro_sessions WHERE date(completedAt) = date(:date) ORDER BY completedAt DESC")
    fun getSessionsByDate(date: String): kotlinx.coroutines.flow.Flow<List<SessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<SessionEntity>>
}

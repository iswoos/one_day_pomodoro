package com.studio.one_day_pomodoro.data.repository

import com.studio.one_day_pomodoro.data.database.PomodoroDao
import com.studio.one_day_pomodoro.data.mapper.toDomain
import com.studio.one_day_pomodoro.data.mapper.toEntity
import com.studio.one_day_pomodoro.domain.model.DailySummary
import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import com.studio.one_day_pomodoro.domain.repository.PomodoroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class PomodoroRepositoryImpl @Inject constructor(
    private val pomodoroDao: PomodoroDao
) : PomodoroRepository {

    override suspend fun saveSession(session: PomodoroSession) {
        pomodoroDao.insertSession(session.toEntity())
    }

    override fun getSessionsByDate(date: LocalDate): Flow<List<PomodoroSession>> {
        return pomodoroDao.getSessionsByDate(date.toString()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDailySummary(date: LocalDate): Flow<DailySummary> {
        return getSessionsByDate(date).map { sessions ->
            val totalMinutes = sessions.sumOf { it.focusDurationInMinutes }
            val sessionCounts = sessions.groupBy { it.purpose }
                .mapValues { (_, list) -> list.size }
            val totalTimes = sessions.groupBy { it.purpose }
                .mapValues { (_, list) -> list.sumOf { it.focusDurationInMinutes } }
            
            DailySummary(
                totalFocusMinutes = totalMinutes,
                sessionCountByPurpose = sessionCounts,
                totalTimeByPurpose = totalTimes
            )
        }
    }
}

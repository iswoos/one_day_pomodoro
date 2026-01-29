package com.studio.one_day_pomodoro.domain.repository

import com.studio.one_day_pomodoro.domain.model.DailySummary
import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 뽀모도로 세션 기록을 관리하는 저장소 인터페이스입니다.
 */
interface PomodoroRepository {
    /**
     * 새로운 세션 기록을 저장합니다.
     */
    suspend fun saveSession(session: PomodoroSession)

    /**
     * 특정 날짜의 모든 세션 기록을 가져옵니다.
     */
    fun getSessionsByDate(date: LocalDate): Flow<List<PomodoroSession>>

    /**
     * 오늘 하루의 집중 통계 요약을 가져옵니다.
     */
    fun getDailySummary(date: LocalDate): Flow<DailySummary>
}

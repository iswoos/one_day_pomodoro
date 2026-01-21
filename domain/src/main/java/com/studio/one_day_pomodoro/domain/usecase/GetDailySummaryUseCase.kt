package com.studio.one_day_pomodoro.domain.usecase

import com.studio.one_day_pomodoro.domain.model.DailySummary
import com.studio.one_day_pomodoro.domain.repository.PomodoroRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * 특정 날짜의 일일 집중 요약을 가져오는 유스케이스입니다.
 */
class GetDailySummaryUseCase @Inject constructor(
    private val repository: PomodoroRepository
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<DailySummary> {
        return repository.getDailySummary(date)
    }
}

package com.studio.one_day_pomodoro.domain.usecase

import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import com.studio.one_day_pomodoro.domain.repository.PomodoroRepository
import javax.inject.Inject

/**
 * 뽀모도로 집중 세션을 저장하는 유스케이스입니다.
 */
class SavePomodoroSessionUseCase @Inject constructor(
    private val repository: PomodoroRepository
) {
    suspend operator fun invoke(session: PomodoroSession) {
        repository.saveSession(session)
    }
}

package com.studio.one_day_pomodoro.data.mapper

import com.studio.one_day_pomodoro.data.database.SessionEntity
import com.studio.one_day_pomodoro.domain.model.PomodoroSession

/**
 * DB 엔티티와 도메인 모델 간의 변환을 담당하는 매퍼입니다.
 */
fun SessionEntity.toDomain(): PomodoroSession {
    return PomodoroSession(
        id = id,
        purpose = purpose,
        focusDurationInMinutes = focusDurationInMinutes,
        completedAt = completedAt
    )
}

fun PomodoroSession.toEntity(): SessionEntity {
    return SessionEntity(
        id = id,
        purpose = purpose,
        focusDurationInMinutes = focusDurationInMinutes,
        completedAt = completedAt
    )
}

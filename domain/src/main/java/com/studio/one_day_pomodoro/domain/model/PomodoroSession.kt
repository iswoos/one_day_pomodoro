package com.studio.one_day_pomodoro.domain.model

import java.time.LocalDateTime

/**
 * 완료된 뽀모도로 세션 기록을 나타내는 데이터 클래스입니다.
 *
 * @property id 고유 식별자 (DB 연동 시 사용)
 * @property purpose 세션의 목적 (유형)
 * @property focusDurationInMinutes 집중 시간 (분)
 * @property completedAt 세션이 완료된 시각
 */
data class PomodoroSession(
    val id: Long = 0,
    val purpose: PomodoroPurpose,
    val focusDurationInMinutes: Int,
    val completedAt: LocalDateTime = LocalDateTime.now()
)

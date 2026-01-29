package com.studio.one_day_pomodoro.domain.model

/**
 * 사용자의 오늘 하루 집중 통계 정보를 담는 데이터 클래스입니다.
 *
 * @property totalFocusMinutes 오늘 총 집중 시간 (분)
 * @property sessionCountByPurpose 목적별 완료된 세션 횟수 맵
 * @property totalTimeByPurpose 목적별 총 집중 시간 맵
 */
data class DailySummary(
    val totalFocusMinutes: Int,
    val sessionCountByPurpose: Map<PomodoroPurpose, Int>,
    val totalTimeByPurpose: Map<PomodoroPurpose, Int>
)

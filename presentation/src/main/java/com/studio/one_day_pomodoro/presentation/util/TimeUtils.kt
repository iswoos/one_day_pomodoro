package com.studio.one_day_pomodoro.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.studio.one_day_pomodoro.presentation.R

/**
 * 분 단위를 받아서 현지화된 "X시간 Y분" (또는 "Xh Ym") 형태의 문자열을 반환합니다.
 */
@Composable
fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    
    val hourUnit = stringResource(R.string.common_unit_hour)
    val minuteUnit = stringResource(R.string.common_unit_min)
    
    return when {
        h > 0 && m > 0 -> "$h$hourUnit $m$minuteUnit"
        h > 0 -> "$h$hourUnit"
        else -> "$m$minuteUnit"
    }
}

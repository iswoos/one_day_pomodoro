package com.studio.one_day_pomodoro.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.presentation.R

/**
 * [PomodoroPurpose]를 각 언어에 맞는 문자열 리소스로 변환하는 확장 함수입니다.
 */
@Composable
fun PomodoroPurpose.getDisplayName(): String {
    return when (this) {
        PomodoroPurpose.STUDY -> stringResource(R.string.purpose_study)
        PomodoroPurpose.READ -> stringResource(R.string.purpose_read)
        PomodoroPurpose.WORK -> stringResource(R.string.purpose_work)
        PomodoroPurpose.HEALTH -> stringResource(R.string.purpose_health)
        PomodoroPurpose.OTHERS -> stringResource(R.string.purpose_others)
    }
}

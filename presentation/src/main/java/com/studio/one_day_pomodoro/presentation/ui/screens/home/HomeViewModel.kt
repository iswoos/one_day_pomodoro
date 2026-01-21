package com.studio.one_day_pomodoro.presentation.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.DailySummary
import com.studio.one_day_pomodoro.domain.usecase.GetDailySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDailySummaryUseCase: GetDailySummaryUseCase
) : ViewModel() {

    // 오늘 하루 집중 요약 정보를 스트림으로 관리합니다.
    val dailySummary: StateFlow<DailySummary?> = getDailySummaryUseCase(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}

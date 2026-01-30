package com.studio.one_day_pomodoro.presentation.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.one_day_pomodoro.domain.model.DailySummary
import com.studio.one_day_pomodoro.domain.repository.TimerStateRepository
import com.studio.one_day_pomodoro.domain.usecase.GetDailySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDailySummaryUseCase: GetDailySummaryUseCase,
    private val timerRepository: TimerStateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 오늘 하루 집중 요약 정보를 스트림으로 관리합니다.
    val dailySummary: StateFlow<DailySummary?> = getDailySummaryUseCase(LocalDate.now())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // Check if timer is running (for cleanup detection)
    suspend fun isTimerRunning(): Boolean {
        return timerRepository.isRunning.first()
    }
    
    // Clean up orphaned timer (after process death)
    fun cleanupOrphanedTimer() {
        viewModelScope.launch {
            timerRepository.stop()
            stopTimerService()
        }
    }
    
    private fun stopTimerService() {
        val intent = Intent().apply {
            setClassName(context, "com.studio.one_day_pomodoro.service.TimerService")
        }
        context.stopService(intent)
    }
}

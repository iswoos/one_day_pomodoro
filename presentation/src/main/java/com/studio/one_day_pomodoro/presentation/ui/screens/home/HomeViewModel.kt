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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailySummary: StateFlow<DailySummary?> = _currentDate
        .flatMapLatest { date -> getDailySummaryUseCase(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun refreshDate() {
        val now = LocalDate.now()
        if (_currentDate.value != now) {
            _currentDate.value = now
        }
    }
    
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

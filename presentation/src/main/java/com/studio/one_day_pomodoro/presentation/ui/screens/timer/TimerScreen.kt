package com.studio.one_day_pomodoro.presentation.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.presentation.ui.components.ads.BannerAdView

@Composable
fun TimerScreen(
    purpose: PomodoroPurpose,
    onBreakStart: (Int) -> Unit,
    onSummaryClick: (PomodoroPurpose, Int) -> Unit,
    onStopClick: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val remainingTime by viewModel.remainingTimeSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startTimer(purpose)
    }

    LaunchedEffect(Unit) {
        viewModel.timerEvent.collect { event ->
            when (event) {
                is TimerViewModel.TimerEvent.GoToBreak -> onBreakStart(event.minutes)
                is TimerViewModel.TimerEvent.Finished -> {
                    onSummaryClick(event.purpose, event.minutes)
                }
            }
        }
    }

    Scaffold(
        bottomBar = { BannerAdView() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = purpose.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                val totalTimeSeconds by viewModel.totalFocusTimeSeconds.collectAsState()
                val progress = if (totalTimeSeconds > 0) remainingTime.toFloat() / totalTimeSeconds else 0f
                
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(280.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    Text(
                        text = formatTime(remainingTime),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 64.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // 일시정지 / 시작 버튼
                    FilledIconButton(
                        onClick = { viewModel.toggleTimer() },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // 중단 버튼
                    OutlinedIconButton(
                        onClick = onStopClick,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

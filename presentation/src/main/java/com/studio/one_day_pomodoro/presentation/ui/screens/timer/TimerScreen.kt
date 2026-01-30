package com.studio.one_day_pomodoro.presentation.ui.screens.timer


import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose

@Composable
fun TimerScreen(
    purpose: PomodoroPurpose,
    onBreakStart: (focusMinutes: Int, completedSessions: Int, totalSessions: Int) -> Unit,
    onSummaryClick: (PomodoroPurpose, Int) -> Unit,
    onStopClick: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val remainingTime by viewModel.remainingTimeSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val context = LocalContext.current
    
    // Handle system back button
    BackHandler {
        viewModel.stopTimer(createEvent = false)
        onStopClick()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 권한 결과 처리
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.startTimer(purpose)
    }

    LaunchedEffect(Unit) {
        viewModel.timerEvent.collect { event ->
            when (event) {
                is TimerViewModel.TimerEvent.GoToBreak -> {
                    // repeatCount 값이 있는지 확인 필요. ViewModel의 settings 참조
                    val settings = viewModel.settings.value
                    val totalSessions = settings?.repeatCount ?: 4
                    val remainingSessions = viewModel.remainingRepeatCount.value
                    val completedSessions = if (totalSessions > 0) totalSessions - remainingSessions else 0
                    onBreakStart(event.minutes, completedSessions, totalSessions)
                }
                is TimerViewModel.TimerEvent.Finished -> {
                    onSummaryClick(event.purpose, event.minutes)
                }
            }
        }
    }

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 세션 카운터
            val settingsState = viewModel.settings.collectAsState()
            val settings = settingsState.value
            val totalSessions = settings?.repeatCount ?: 4
            val remainingSessions by viewModel.remainingRepeatCount.collectAsState()
            val completedSessions = if (totalSessions > 0) totalSessions - remainingSessions else 0
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "완료된 세션: $completedSessions / $totalSessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            val totalTimeSeconds by viewModel.totalFocusTimeSeconds.collectAsState()
            val progress = if (totalTimeSeconds > 0) remainingTime.toFloat() / totalTimeSeconds else 0f
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(320.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 16.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatTime(remainingTime),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 72.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = purpose.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 일시정지 / 시작 버튼 (둥근 스타일)
                Button(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 140.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "일시정지" else "시작",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 중단 버튼 (아이콘만)
                IconButton(
                    onClick = {
                        viewModel.stopTimer(createEvent = false)
                        onStopClick()
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "중단",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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


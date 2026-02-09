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
    onStopClick: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = androidx.compose.runtime.remember(context) { 
        context.let { 
            var c = it
            while (c is android.content.ContextWrapper) {
                if (c is android.app.Activity) break
                c = c.baseContext
            }
            c as? android.app.Activity
        }
    }

    LaunchedEffect(Unit) {
        activity?.let { com.studio.one_day_pomodoro.presentation.ui.components.ads.InterstitialAdHelper.loadAd(it) }
    }

    val remainingTime by viewModel.remainingTimeSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    
    // Handle system back button
    BackHandler {
        viewModel.stopTimer(createEvent = false)
        onStopClick()
    }


    LaunchedEffect(Unit) {
        // 권한 확인은 HomeScreen에서 이미 완료됨
        viewModel.startTimer(purpose)
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
            val totalSessions by viewModel.totalSessions.collectAsState()
            val completedSessions by viewModel.completedSessions.collectAsState()
            
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
            
            val focusDurationMin by viewModel.focusDurationMinutes.collectAsState()
            val totalTimeSeconds = focusDurationMin * 60L
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


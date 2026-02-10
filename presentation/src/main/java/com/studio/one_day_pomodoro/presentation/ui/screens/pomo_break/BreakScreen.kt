package com.studio.one_day_pomodoro.presentation.ui.screens.pomo_break

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.res.stringResource
import com.studio.one_day_pomodoro.presentation.R
import com.studio.one_day_pomodoro.presentation.util.formatDuration

@Composable
fun BreakScreen(
    focusMinutes: Int,
    completedSessions: Int,
    totalSessions: Int,
    onStopClick: () -> Unit,
    viewModel: BreakViewModel = hiltViewModel()
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

    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val breakDurationMin by viewModel.breakDurationMinutes.collectAsState()
    val totalBreakSeconds = breakDurationMin * 60L
    val progress = if (totalBreakSeconds > 0) remainingSeconds.toFloat() / totalBreakSeconds else 0f
    
    val currentCompletedSessions by viewModel.completedSessions.collectAsState()
    val currentTotalSessions by viewModel.totalSessions.collectAsState()
    
    // Handle system back button
    BackHandler {
        viewModel.stopBreak()
        onStopClick()
    }

    val timerMode by viewModel.timerMode.collectAsState()

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
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.timer_completed_sessions, currentCompletedSessions, currentTotalSessions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.break_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.break_current_session, formatDuration(focusMinutes)), 
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // 중앙 원형 프로그레스
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(280.dp),
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
                        text = formatTime(remainingSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.break_remaining_time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 집중 종료 버튼 (빨간색)
            Button(
                onClick = {
                    viewModel.stopBreak()
                    onStopClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(R.string.break_stop),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

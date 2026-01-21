package com.studio.one_day_pomodoro.presentation.ui.screens.pomo_break

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studio.one_day_pomodoro.presentation.ui.components.ads.BannerAdView

@Composable
fun BreakScreen(
    focusMinutes: Int,
    onBreakEnd: () -> Unit,
    viewModel: BreakViewModel = hiltViewModel()
) {
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val totalBreakSeconds by viewModel.totalBreakSeconds.collectAsState()
    val progress = if (totalBreakSeconds > 0) remainingSeconds.toFloat() / totalBreakSeconds else 0f

    LaunchedEffect(Unit) {
        viewModel.startBreak()
    }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds <= 0 && totalBreakSeconds > 0) {
            onBreakEnd()
        }
    }

    Scaffold(
        bottomBar = { BannerAdView() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "훌륭해요! 이제 잠시 쉬어보세요.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "이번 세션: ${focusMinutes}분 완료", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // 중앙 원형 프로그레스
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(240.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(remainingSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "남은 시간",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = "📢 광고는 뽀모 운영에 큰 힘이 됩니다",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

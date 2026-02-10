package com.studio.one_day_pomodoro.presentation.ui.screens.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



import androidx.compose.ui.res.stringResource
import com.studio.one_day_pomodoro.presentation.R
import com.studio.one_day_pomodoro.presentation.util.getDisplayName
import com.studio.one_day_pomodoro.presentation.util.formatDuration

@Composable
fun SummaryScreen(
    purpose: PomodoroPurpose,
    minutes: Int,
    onConfirmClick: () -> Unit
) {
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
            Text(
                text = stringResource(R.string.summary_msg_purpose, purpose.getDisplayName()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.summary_msg_total, formatDuration(minutes)),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onConfirmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = stringResource(R.string.common_done), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

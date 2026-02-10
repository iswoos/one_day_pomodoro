package com.studio.one_day_pomodoro.presentation.ui.screens.home

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.studio.one_day_pomodoro.presentation.R
import com.studio.one_day_pomodoro.presentation.util.getDisplayName
import com.studio.one_day_pomodoro.presentation.util.formatDuration

@Composable
fun HomeScreen(
    onStartClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val summary by viewModel.dailySummary.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val context = LocalContext.current
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    
    // 앱 시작 시 날짜 갱신
    LaunchedEffect(Unit) {
        viewModel.refreshDate()
    }
    // --- Permission Handlers ---
    
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    // Launcher for Notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // After interaction (granted or denied), re-run the check. 
        performPermissionCheck(context, null, alarmManager, { showExactAlarmDialog = true }, onStartClick)
    }

    // Launcher for Exact Alarm settings (Android 12+)
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Return from settings: re-run the full check
        performPermissionCheck(context, notificationPermissionLauncher, alarmManager, { showExactAlarmDialog = true }, onStartClick)
    }

    // 정확한 알람 권한 안내 다이얼로그
    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(text = stringResource(R.string.permission_alarm_title), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.permission_alarm_message)) },
            confirmButton = {
                Button(onClick = {
                    showExactAlarmDialog = false
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    exactAlarmLauncher.launch(intent)
                }) { Text(stringResource(R.string.common_go_to_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = onSettingsClick) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_today_focus),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = currentDate.format(DateTimeFormatter.ofPattern(
                    if (Locale.getDefault().language == "ko") "M월 d일 (E)" else "EEE, MMM d, yyyy",
                    Locale.getDefault()
                )),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(R.string.home_total_focus_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(summary?.totalFocusMinutes ?: 0),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    performPermissionCheck(context, notificationPermissionLauncher, alarmManager, { showExactAlarmDialog = true }, onStartClick)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = stringResource(R.string.home_start_focus), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(text = stringResource(R.string.home_performance_by_category), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
           
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PomodoroPurpose.entries.forEach { purpose ->
                    val time = summary?.totalTimeByPurpose?.get(purpose) ?: 0
                    val count = summary?.sessionCountByPurpose?.get(purpose) ?: 0
                    PurposeSummaryItem(purpose, time, count)
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun PurposeSummaryItem(purpose: PomodoroPurpose, minutes: Int, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = purpose.getDisplayName(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = formatDuration(minutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "${count}${stringResource(R.string.common_unit_count)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun performPermissionCheck(
    context: Context,
    notificationLauncher: ActivityResultLauncher<String>?,
    alarmManager: AlarmManager?,
    onShowExactAlarmDialog: () -> Unit,
    onStartClick: () -> Unit
) {
    // 1. Notification Permission (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
            != PackageManager.PERMISSION_GRANTED) {
            notificationLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
    }

    // 2. Exact Alarm Permission (Android 12+)
    // !IMPORTANT: Must check for Android 12+ AND verify if permission is granted.
    // If alarmManager is null, we safely exit or try to proceed, but on real S+ devices it shouldn't be null.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val canSchedule = alarmManager?.canScheduleExactAlarms() ?: false
        if (!canSchedule) {
            onShowExactAlarmDialog()
            return
        }
    }

    // 3. Success -> Proceed
    onStartClick()
}

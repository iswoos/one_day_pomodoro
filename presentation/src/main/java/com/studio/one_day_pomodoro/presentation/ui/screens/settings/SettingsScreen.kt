package com.studio.one_day_pomodoro.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "설정",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            settings?.let { s ->
                SettingItem(
                    label = "집중 시간", 
                    value = s.focusMinutes,
                    unit = "분",
                    onValueChange = { viewModel.setFocusMinutes(it) },
                    onDecrease = { viewModel.updateFocusMinutes(-1) },
                    onIncrease = { viewModel.updateFocusMinutes(1) }
                )
                SettingItem(
                    label = "휴식 시간", 
                    value = s.breakMinutes,
                    unit = "분",
                    onValueChange = { viewModel.setBreakMinutes(it) },
                    onDecrease = { viewModel.updateBreakMinutes(-1) },
                    onIncrease = { viewModel.updateBreakMinutes(1) }
                )
                SettingItem(
                    label = "시도 회수", 
                    value = s.repeatCount,
                    unit = "회",
                    onValueChange = { viewModel.setRepeatCount(it) },
                    onDecrease = { viewModel.updateRepeatCount(-1) },
                    onIncrease = { viewModel.updateRepeatCount(1) }
                )

                VibrationSettingItem(
                    enabled = s.vibrationEnabled,
                    intensity = s.vibrationIntensity,
                    onEnabledChange = { viewModel.toggleVibrationEnabled(it) },
                    onIntensityChange = { viewModel.setVibrationIntensity(it) }
                )
            } ?: Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "모든 설정값은 최소 1분/1회 이상이어야 합니다.\n시도 회수는 총 집중 세션의 수를 의미합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveSettings()
                    onSaveClick() // 상위에서 navController.popBackStack() 처리 등을 가정
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(text = "저장", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingItem(
    label: String, 
    value: Int,
    unit: String,
    onValueChange: (String) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    // 내부 상태를 두어 입력 도중 값이 비거나 변경되는 것을 자연스럽게 처리
    var textState by remember(value) { mutableStateOf(value.toString()) }
    var isFocused by remember { mutableStateOf(false) }

    // 만약 외부에서 value가 바뀌었는데 포커스 중이 아니라면 textState 업데이트 (ex: 버튼으로 증감 시)
    LaunchedEffect(value) {
        if (!isFocused) {
            textState = value.toString()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease) {
                Icon(Icons.Default.Remove, contentDescription = null)
            }
            
            OutlinedTextField(
                value = textState,
                onValueChange = { 
                    textState = it
                    // 입력 중에는 즉시 ViewModel에 반영 (유효성 검사는 ViewModel에서 처리됨)
                    if (it.isNotEmpty()) {
                        onValueChange(it)
                    }
                },
                modifier = Modifier
                    .width(70.dp)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && !isFocused) {
                            // 포커스 얻었을 때 -> 입력 편의를 위해 전체 선택 혹은 비우기
                            // 요청사항: "초기화된 공란에 입력되는 형태"
                            textState = ""
                        } else if (!focusState.isFocused && isFocused) {
                            // 포커스 잃었을 때 -> 비어있으면 원복
                            if (textState.isEmpty()) {
                                textState = value.toString()
                                onValueChange(value.toString())
                            }
                        }
                        isFocused = focusState.isFocused
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            
            Text(
                text = unit, 
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
            )

            IconButton(onClick = onIncrease) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
fun VibrationSettingItem(
    enabled: Boolean,
    intensity: Float,
    onEnabledChange: (Boolean) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "진동 알림", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "진동 세기", 
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray,
                modifier = Modifier.width(80.dp)
            )
            
            Slider(
                value = intensity,
                onValueChange = onIntensityChange,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                onValueChangeFinished = {
                    // 슬라이더 조절이 끝날 때 진동 피드백 제공
                    if (enabled) {
                        val amplitude = ((intensity * 254).toInt() + 1).coerceAtMost(255)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, amplitude))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(200)
                        }
                    }
                }
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

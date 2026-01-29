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
                        text = "집중 설정",
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
                onClick = onSaveClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(text = "저장 및 완료", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    onValueChange(it)
                },
                modifier = Modifier.width(70.dp),
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

package com.studio.one_day_pomodoro.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import com.studio.one_day_pomodoro.MainActivity
import com.studio.one_day_pomodoro.R
import com.studio.one_day_pomodoro.domain.model.PomodoroPurpose
import com.studio.one_day_pomodoro.domain.model.PomodoroSession
import com.studio.one_day_pomodoro.domain.model.TimerMode
import com.studio.one_day_pomodoro.domain.usecase.SavePomodoroSessionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerRepository: com.studio.one_day_pomodoro.domain.repository.TimerStateRepository
    
    @Inject
    lateinit var savePomodoroSessionUseCase: SavePomodoroSessionUseCase

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 알림 관리를 위한 변수
    private val notificationId = 1
    private val channelId = "timer_channel"
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var vibrator: Vibrator
    
    private var isLastSession = false
    private var currentMode: TimerMode = TimerMode.NONE
    private var currentPurpose: PomodoroPurpose = PomodoroPurpose.OTHERS
    
    private var breakDurationSeconds: Long = 5 * 60L
    private var focusDurationMinutes: Int = 25

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val durationMinutes = intent?.getIntOfExtra("DURATION_MINUTES", 25) ?: 25
            focusDurationMinutes = durationMinutes
            isLastSession = intent?.getBooleanExtra("IS_LAST_SESSION", false) ?: false
            
            val breakMinutes = intent?.getIntOfExtra("BREAK_DURATION_MINUTES", 5) ?: 5
            breakDurationSeconds = breakMinutes * 60L
            
            val purposeName = intent?.getStringExtra("PURPOSE") ?: "OTHERS"
            currentPurpose = try {
                PomodoroPurpose.valueOf(purposeName)
            } catch (e: Exception) {
                PomodoroPurpose.OTHERS
            }
            
            // Set mode immediately from intent
            val modeName = intent?.getStringExtra("TIMER_MODE") ?: TimerMode.NONE.name
            currentMode = try {
                TimerMode.valueOf(modeName)
            } catch (e: Exception) {
                TimerMode.NONE
            }
            
            // 새로운 타이머 시작 시 이전 완료 알림 제거
            notificationManager.cancel(2)
            
            // 초기 알림 표시 및 포그라운드 시작
            startForeground(notificationId, createNotification(durationMinutes * 60L))
            
            // 타이머 시작 시 진동 (소리 없이 알림만 갱신하는 것이 아니라 명시적으로 진동)
            // 단, Auto-Switching인 경우 여기서 진동이 울리면 좋음.
            // 하지만 onStartCommand는 TimerViewModel에서 startService할 때만 불림.
            // 내부 전환 시에는 onStartCommand가 안 불림.
            triggerVibration(VibrationPattern.START)
            
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf() 
        }
        
        return START_STICKY
    }
    
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            notificationManager = getSystemService(NotificationManager::class.java)
            createNotificationChannel()
            
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            
            val powerManager = getSystemService(PowerManager::class.java)
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OneDayPomodoro:TimerLock")
            wakeLock.acquire(4 * 60 * 60 * 1000L) 
            
            observeTimerState()
            observeTimerMode()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            notificationManager.cancel(notificationId) 
            if (::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "뽀모 타이머", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "타이머 실행 중 알림"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun observeTimerState() {
        serviceScope.launch {
            timerRepository.remainingSeconds.collect { seconds ->
                updateNotification(seconds)
            }
        }
        
        serviceScope.launch {
            timerRepository.isRunning.collect { isRunning ->
                if (!isRunning) {
                     val seconds = timerRepository.remainingSeconds.value
                     val shouldAutoTransition = seconds <= 0 && currentMode == TimerMode.FOCUS && !isLastSession
                     
                     if (shouldAutoTransition) {
                         transitionToBreak()
                     } else {
                         stopForeground(STOP_FOREGROUND_REMOVE)
                         stopSelf()
                     }
                }
            }
        }
    }
    
    private fun observeTimerMode() {
        serviceScope.launch {
            timerRepository.timerMode.collect { mode ->
                currentMode = mode
                // 모드 변경 시 알림 즉시 갱신 (제목 변경 등)
                // remainingSeconds가 변경되지 않아도 모드가 바뀌면 갱신 필요
                val seconds = timerRepository.remainingSeconds.value
                if (seconds > 0) {
                    notificationManager.notify(notificationId, createNotification(seconds))
                }
            }
        }
    }
    
    private fun transitionToBreak() {
        serviceScope.launch {
            // 1. Save Session
            savePomodoroSessionUseCase(
                PomodoroSession(
                    purpose = currentPurpose,
                    focusDurationInMinutes = focusDurationMinutes,
                    completedAt = LocalDateTime.now()
                )
            )
            
            // 2. Start Break Timer
            // 진동: 완료 진동을 여기서 줘야 함 (updateNotification에서는 0초일 때 스킵했으므로)
            triggerVibration(VibrationPattern.COMPLETE)
            
            timerRepository.start(breakDurationSeconds, TimerMode.BREAK)
            // Repository.start sets isRunning=true, so the loop continues.
        }
    }

    private fun updateNotification(seconds: Long) {
        if (seconds > 0) {
            notificationManager.notify(notificationId, createNotification(seconds))
        } else {
             // 0초 (완료 시점)
             val shouldAutoTransition = currentMode == TimerMode.FOCUS && !isLastSession
             
             if (shouldAutoTransition) {
                 // 전환 예정이므로 "완료/휴식하세요" 알림을 띄우지 않음.
                 // 곧바로 Break 모드로 전환되어 초가 갱신될 것임.
                 return
             }
             
             // 완료 시 진동 (마지막 세션이거나 휴식 끝날 때)
             val vibrationPattern = if (isLastSession) {
                 VibrationPattern.FINAL_COMPLETE
             } else {
                 VibrationPattern.COMPLETE
             }
             triggerVibration(vibrationPattern)
             
             // 완료 알림
             val contentText = if (isLastSession) "모든 세션이 완료되었습니다." else "휴식이 종료되었습니다."
             val titleText = if (isLastSession) "집중 완료!" else "휴식 완료!"
             
             val completeNotification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("TIMER_FINISHED_MODE", currentMode.name)
                    }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .build()
            
            notificationManager.notify(2, completeNotification)
            notificationManager.cancel(notificationId)
        }
    }

    private fun createNotification(seconds: Long): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedTime = formatTime(seconds)
        
        val title = when (currentMode) {
            TimerMode.FOCUS -> "집중 중입니다"
            TimerMode.BREAK -> "휴식 시간입니다"
            else -> "타이머 실행 중"
        }
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(formattedTime)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) 
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                NotificationCompat.Action(
                    0, "앱 열기", pendingIntent
                )
            )
            .build()
    }
    
    private fun triggerVibration(pattern: VibrationPattern) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern.timings, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private enum class VibrationPattern(val timings: LongArray) {
        START(longArrayOf(0, 200)),
        COMPLETE(longArrayOf(0, 500)), // 조금 더 길게
        FINAL_COMPLETE(longArrayOf(0, 200, 100, 200, 100, 400))
    }
    
    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    private fun Intent.getIntOfExtra(name: String, defaultValue: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getIntExtra(name, defaultValue)
        } else {
            getIntExtra(name, defaultValue)
        }
    }
}

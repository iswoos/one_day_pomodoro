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
    
    // private var isLastSession = false // 제거: repository.completedSessions 활용
    // private var currentPurpose 를 제거하고 repository 값을 사용하도록 변경 예정이나, 전환 시에는 로컬 캐시가 필요할 수도 있음.
    // 하지만 가급적 repository를 관찰하도록 함.
    
    private lateinit var alarmManager: AlarmManager
    private var alarmPendingIntent: PendingIntent? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            notificationManager.cancel(2)
            
            val seconds = if (intent?.action == "ACTION_TIMER_EXPIRED") {
                handleTimerExpired()
                0L
            } else {
                val modeName = intent?.getStringExtra("TIMER_MODE") ?: timerRepository.timerMode.value.name
                val mode = try { TimerMode.valueOf(modeName) } catch (e: Exception) { TimerMode.NONE }

                val durationMinutes = intent?.getIntExtra("DURATION_MINUTES", timerRepository.focusDurationMinutes.value) ?: 25
                val breakMinutes = intent?.getIntExtra("BREAK_DURATION_MINUTES", timerRepository.breakDurationMinutes.value) ?: 5
                val totalSessions = intent?.getIntExtra("TOTAL_SESSIONS", timerRepository.totalSessions.value) ?: 1
                val completedSessions = intent?.getIntExtra("COMPLETED_SESSIONS", timerRepository.completedSessions.value) ?: 0
                
                val purposeName = intent?.getStringExtra("PURPOSE") ?: timerRepository.currentPurpose.value.name
                val purpose = try { PomodoroPurpose.valueOf(purposeName) } catch (e: Exception) { PomodoroPurpose.OTHERS }

                val totalSeconds = if (mode == TimerMode.FOCUS) durationMinutes * 60L else breakMinutes * 60L
                
                // 만약 앱 실행 중 다시 호출되었는데, 이미 같은 모드라면 상태를 뭉개지 않음 (중요!)
                if (timerRepository.isRunning.value && timerRepository.timerMode.value == mode && timerRepository.remainingSeconds.value > 0) {
                    timerRepository.remainingSeconds.value
                } else {
                    timerRepository.start(
                        seconds = totalSeconds,
                        mode = mode,
                        focusMin = durationMinutes,
                        breakMin = breakMinutes,
                        total = totalSessions,
                        completed = completedSessions,
                        purpose = purpose
                    )
                    totalSeconds
                }
            }
            
            startForeground(notificationId, createNotification(seconds))
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
            alarmManager = getSystemService(AlarmManager::class.java)
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
                     
                     // 1. Focus Finished -> Break (unless last)
                     // 2. Break Finished -> Focus (unless last? wait, Break is never last basically, Focus is last)
                     
                     val isLast = timerRepository.completedSessions.value >= timerRepository.totalSessions.value - 1
                     val isFocusFinished = seconds <= 0 && timerRepository.timerMode.value == TimerMode.FOCUS && !isLast
                     val isBreakFinished = seconds <= 0 && timerRepository.timerMode.value == TimerMode.BREAK
                     
                      if (isFocusFinished) {
                         transitionToBreak()
                     } else if (isBreakFinished) {
                         transitionToNextFocus()
                     } else if (seconds <= 0) {
                         // 그 외 (마지막 세션 종료, 혹은 유저 중단)
                         cancelAlarm()
                         stopForeground(STOP_FOREGROUND_REMOVE)
                         stopSelf()
                     }
                } else {
                    // 타이머 시작/재개됨
                    val seconds = timerRepository.remainingSeconds.value
                    if (seconds > 0) {
                        scheduleAlarm(seconds)
                    }
                }
            }
        }
    }
    
    private fun observeTimerMode() {
        serviceScope.launch {
                val mode = timerRepository.timerMode.value
                val seconds = timerRepository.remainingSeconds.value
                if (seconds > 0) {
                    notificationManager.notify(notificationId, createNotification(seconds))
                }
        }
    }
    
    private fun transitionToBreak() {
        serviceScope.launch {
            savePomodoroSessionUseCase(
                PomodoroSession(
                    purpose = timerRepository.currentPurpose.value,
                    focusDurationInMinutes = timerRepository.focusDurationMinutes.value,
                    completedAt = LocalDateTime.now()
                )
            )
            triggerVibration(VibrationPattern.COMPLETE)
            
            val newCompleted = timerRepository.completedSessions.value + 1
            timerRepository.start(
                seconds = timerRepository.breakDurationMinutes.value * 60L,
                mode = TimerMode.BREAK,
                focusMin = timerRepository.focusDurationMinutes.value,
                breakMin = timerRepository.breakDurationMinutes.value,
                total = timerRepository.totalSessions.value,
                completed = newCompleted,
                purpose = timerRepository.currentPurpose.value
            )
        }
    }
    
    private fun transitionToNextFocus() {
        serviceScope.launch {
            triggerVibration(VibrationPattern.START)
            timerRepository.start(
                seconds = timerRepository.focusDurationMinutes.value * 60L,
                mode = TimerMode.FOCUS,
                focusMin = timerRepository.focusDurationMinutes.value,
                breakMin = timerRepository.breakDurationMinutes.value,
                total = timerRepository.totalSessions.value,
                completed = timerRepository.completedSessions.value,
                purpose = timerRepository.currentPurpose.value
            )
        }
    }

    private fun updateNotification(seconds: Long) {
        if (seconds > 0) {
            notificationManager.notify(notificationId, createNotification(seconds))
        } else {
             // 0초 (완료 시점)
             
             // Auto-Switching 대상이면 알림 띄우지 않고 바로 전환
             val isLast = timerRepository.completedSessions.value >= timerRepository.totalSessions.value - 1
             val isFocusFinished = timerRepository.timerMode.value == TimerMode.FOCUS && !isLast
             val isBreakFinished = timerRepository.timerMode.value == TimerMode.BREAK
             
             if (isFocusFinished || isBreakFinished) {
                 return
             }
             
             // 진짜 마지막 세션 종료 시
             val vibrationPattern = VibrationPattern.FINAL_COMPLETE
             triggerVibration(vibrationPattern)
             
             val contentText = "모든 세션이 완료되었습니다."
             val titleText = "집중 완료!"
             
             val completeNotification = NotificationCompat.Builder(this, channelId)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("TIMER_FINISHED_MODE", timerRepository.timerMode.value.name)
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
        
        val title = when (timerRepository.timerMode.value) {
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
        COMPLETE(longArrayOf(0, 500)), 
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

    private fun handleTimerExpired() {
        // 알람에 의해 호출됨. 만약 레포지토리가 아직 동작 중이라면 강제로 0으로 만들어서 완료 트리거
        if (timerRepository.isRunning.value && timerRepository.remainingSeconds.value > 0) {
            // repository.stop() 또는 직접 초 갱신? 
            // RepositoryImpl을 수정해서 초를 바로 조정할 수 있는 기능이 없으므로
            // 여기서는 repository.stop()은 하지 않고, transition logic을 직접 수행하거나
            // repository에 finish() 기능 추가 필요.
            // 일단은 transitionToBreak/NextFocus 로직이 observeTimerState에서 
            // isRunning=false && seconds<=0 일 때 동작하도록 되어 있으므로
            // repository.stop()을 하면 seconds가 0이 아닐 수 있음.
            
            // 따라서 repository.start(0, currentMode)를 호출하면 
            // 즉시 isRunning=false && remaining=0 이 되어 observer가 동작함.
            timerRepository.start(
                seconds = 0,
                mode = timerRepository.timerMode.value,
                focusMin = timerRepository.focusDurationMinutes.value,
                breakMin = timerRepository.breakDurationMinutes.value,
                total = timerRepository.totalSessions.value,
                completed = timerRepository.completedSessions.value,
                purpose = timerRepository.currentPurpose.value
            )
        }
    }

    private fun scheduleAlarm(seconds: Long) {
        cancelAlarm()
        
        val intent = Intent(this, AlarmReceiver::class.java)
        alarmPendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerAtMillis = android.os.SystemClock.elapsedRealtime() + (seconds * 1000)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                alarmPendingIntent!!
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                alarmPendingIntent!!
            )
        }
    }

    private fun cancelAlarm() {
        alarmPendingIntent?.let {
            alarmManager.cancel(it)
            alarmPendingIntent = null
        }
    }
}

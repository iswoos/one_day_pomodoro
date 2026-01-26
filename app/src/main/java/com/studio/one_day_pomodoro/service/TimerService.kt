package com.studio.one_day_pomodoro.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.studio.one_day_pomodoro.MainActivity
import com.studio.one_day_pomodoro.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerRepository: com.studio.one_day_pomodoro.domain.repository.TimerStateRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 알림 관리를 위한 변수
    private val notificationId = 1
    private val channelId = "timer_channel"
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private var isLastSession = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val durationMinutes = intent?.getIntOfExtra("DURATION_MINUTES", 25) ?: 25
            isLastSession = intent?.getBooleanExtra("IS_LAST_SESSION", false) ?: false
            
            // 새로운 타이머 시작 시 이전 완료 알림 제거
            notificationManager.cancel(2)
            
            // 초기 알림 표시 및 포그라운드 시작
            startForeground(notificationId, createNotification(durationMinutes * 60L))
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf() // 실행 불가 시 종료
        }
        
        return START_NOT_STICKY
    }
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            notificationManager = getSystemService(NotificationManager::class.java)
            createNotificationChannel()
            
            // WakeLock 획득 (잠금 화면 대응)
            val powerManager = getSystemService(PowerManager::class.java)
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OneDayPomodoro:TimerLock")
            wakeLock.acquire(4 * 60 * 60 * 1000L) // 최대 4시간 유지
            
            // 타이머 상태 관찰
            observeTimerState()
        } catch (e: Exception) {
            e.printStackTrace()
            // 서비스 생성 실패 시 종료
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            notificationManager.cancel(notificationId) // 진행 중 알림 제거
            
            if (::wakeLock.isInitialized && wakeLock.isHeld) {
                wakeLock.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
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
                     // 타이머 멈춤 -> 서비스 종료
                     stopForeground(STOP_FOREGROUND_REMOVE)
                     stopSelf()
                }
            }
        }
    }

    private fun updateNotification(seconds: Long) {
        if (seconds > 0) {
            notificationManager.notify(notificationId, createNotification(seconds))
        } else {
             // 완료 시 알림 (ID 2번 사용, 일반 알림)
             val contentText = if (isLastSession) "모든 세션이 완료되었습니다." else "휴식을 취해보세요."
             
             val completeNotification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("집중 완료!")
                .setContentText(contentText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, PendingIntent.FLAG_IMMUTABLE
                ))
                .build()
            
            notificationManager.notify(2, completeNotification)
            notificationManager.cancel(notificationId) // 진행 중 알림 제거
        }
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

    private fun createNotification(seconds: Long): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedTime = formatTime(seconds)
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("집중 중입니다")
            .setContentText(formattedTime)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true) // 알림 갱신 시 소리/진동 방지
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                NotificationCompat.Action(
                    0, "앱 열기", pendingIntent
                )
            )
            .build()
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

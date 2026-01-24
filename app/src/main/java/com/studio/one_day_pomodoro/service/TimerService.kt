package com.studio.one_day_pomodoro.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
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

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        
        // 타이머 상태 관찰
        observeTimerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntOfExtra("DURATION_MINUTES", 25) ?: 25
        
        // 초기 알림 표시 및 포그라운드 시작
        startForeground(notificationId, createNotification(durationMinutes * 60L))
        
        return START_NOT_STICKY
    }

    private fun observeTimerState() {
        serviceScope.launch {
            timerRepository.remainingSeconds.collect { seconds ->
                updateNotification(seconds)
            }
        }
        
        serviceScope.launch {
            timerRepository.isRunning.collect { isRunning ->
                if (!isRunning && timerRepository.remainingSeconds.value <= 0) {
                    // 타이머가 완료되어 멈춘 경우 -> 잠시 후 서비스 종료 (알림은 남겨둘 수도 있음)
                    // 여기서는 즉시 종료보다 사용자가 앱을 열게 유도하는 게 좋음
                    // 일단은 타이머가 멈추면 서비스도 멈추게 (백그라운드 리소스 해제)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
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

    private fun updateNotification(seconds: Long) {
        if (seconds > 0) {
            notificationManager.notify(notificationId, createNotification(seconds))
        } else {
             // 완료 시 알림 (헤드업 알림으로 변경하면 좋음)
             val completeNotification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("집중 완료!")
                .setContentText("휴식을 취해보세요.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                ))
                .build()
            notificationManager.notify(notificationId, completeNotification)
        }
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

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

package com.example.tasktimer.model

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R
import com.example.tasktimer.view.AlarmActivity

@Suppress("DEPRECATION")
class TaskTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "task_timer_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_SUBTASK_NAME = "subtask_name"
        const val EXTRA_SUBTASKS = "subtasks"
    }

    // Переменные для состояния задачи и таймера
    private var taskName: String? = null
    private var subtasks: List<Subtask> = emptyList()
    private var currentSubtaskIndex = 0
    private var remainingTime: Long = 0
    private var currentTimer: CountDownTimer? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d("TaskTimerService", "onCreate: Сервис создан")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TaskTimerService", "onStartCommand: Сервис получен с intent: $intent")
        taskName = intent?.getStringExtra(EXTRA_TASK_NAME)
        subtasks = intent?.getParcelableArrayListExtra(EXTRA_SUBTASKS) ?: emptyList()

        if (subtasks.isNotEmpty()) {
            currentSubtaskIndex = 0
            startSubtaskTimer()
        }

        return START_STICKY // Продолжать работу даже при закрытии приложения
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TaskTimerService", "onDestroy: Сервис уничтожен")
        stopCurrentTimer()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Логика таймера для подзадачи ---
    private fun startSubtaskTimer() {
        Log.d("TaskTimerService", "startSubtaskTimer: Запуск таймера для подзадачи")
        if (currentSubtaskIndex >= subtasks.size) {
            stopSelf()
            return
        }

        val currentSubtask = subtasks[currentSubtaskIndex]
        remainingTime = currentSubtask.duration // Время текущей подзадачи

        // Создаем уведомление
        startForeground(NOTIFICATION_ID, createNotification(formatTime(remainingTime), currentSubtask.description))

        // Запускаем таймер
        currentTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateNotification(formatTime(remainingTime), currentSubtask.description)
            }

            override fun onFinish() {
                Log.d("TaskTimerService", "onFinish: Таймер завершен для подзадачи")
                triggerAlarm(currentSubtask.isHighPriority) // Вызов будильника
                currentSubtaskIndex++
                startSubtaskTimer() // Переход к следующей подзадаче
            }
        }.start()
        isRunning = true
    }

    private fun stopCurrentTimer() {
        Log.d("TaskTimerService", "stopCurrentTimer: Остановка текущего таймера")
        currentTimer?.cancel()
        isRunning = false
    }

    // Форматируем время в формат HH:MM:SS
    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        val time = String.format("%02d:%02d:%02d", hours, minutes, secs)
        Log.d("TaskTimerService", "formatTime: Отформатированное время - $time")
        return time
    }

    // --- Уведомления ---
    private fun createNotification(time: String, subtaskName: String): Notification {
        Log.d("TaskTimerService", "createNotification: Создание уведомления для $subtaskName с временем $time")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Задача: $taskName")
            .setContentText("Подзадача: $subtaskName — $time")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(time: String, subtaskName: String) {
        Log.d("TaskTimerService", "updateNotification: Обновление уведомления с временем $time")
        val notification = createNotification(time, subtaskName)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        Log.d("TaskTimerService", "createNotificationChannel: Создание канала уведомлений")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                description = "Уведомления таймера задач"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // --- Будильник ---
    private fun triggerAlarm(priority: Boolean) {
        Log.d("TaskTimerService", "triggerAlarm: Запуск будильника с приоритетом $priority")
        val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("priority", priority)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent)
    }
}

// --- Ресивер для будильника ---
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val priority = intent?.getIntExtra("priority", 0) ?: 0
        Log.d("AlarmReceiver", "onReceive: Получен будильник с приоритетом $priority")

        // Логика вибрации и звука будильника
        val ringtoneUri = when (priority) {
            1 -> R.raw.basic_alarm_ringtone
            2 -> R.raw.electronic_alarm_signal
            else -> R.raw.basic_alarm_ringtone
        }

        // Получаем AudioManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Сохраняем текущий уровень громкости
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        // Уменьшаем громкость для теста (например, до 20% от текущего уровня)
        val reducedVolume = (currentVolume * 0.2).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, reducedVolume, 0)

        // Воспроизведение звука будильника
        val mediaPlayer = MediaPlayer.create(context, ringtoneUri)
        mediaPlayer.start()

        // Возвращаем громкость в исходное состояние через небольшой промежуток времени
        mediaPlayer.setOnCompletionListener {
            // Восстанавливаем исходный уровень громкости
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0)
        }

        // Вибрация
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }

        // Вызов активити будильника
        val alarmIntent = Intent(context, AlarmActivity::class.java)
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(alarmIntent)
    }
}

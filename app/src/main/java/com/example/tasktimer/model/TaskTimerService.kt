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
        if (currentSubtaskIndex >= subtasks.size) {
            Log.d("TaskTimerService", "Все подзадачи завершены")
            clearAllNotifications()
            stopSelf()
            return
        }

        val currentSubtask = subtasks[currentSubtaskIndex]
        remainingTime = currentSubtask.duration * 1000 // Время подзадачи в миллисекундах

        // Уведомление с обратным отсчетом
        startForeground(
            NOTIFICATION_ID,
            createCountdownNotification(System.currentTimeMillis() + remainingTime, currentSubtask.description)
        )

        // Таймер обратного отсчета
        currentTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                val formattedTime = formatTime(millisUntilFinished)
                updateNotification(formattedTime, currentSubtask.description)
            }

            override fun onFinish() {
                // Вызов будильника после завершения таймера
                triggerAlarm(currentSubtask.isHighPriority)

                // Переключаемся на следующую подзадачу
                currentSubtaskIndex++
                startSubtaskTimer()
            }
        }
        currentTimer?.start()
    }





    private fun stopCurrentTimer() {
        Log.d("TaskTimerService", "stopCurrentTimer: Остановка текущего таймера")
        currentTimer?.cancel()
        isRunning = false
    }

    // Форматируем время в формат HH:MM:SS
    fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
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

    private fun createCountdownNotification(endTime: Long, subtaskName: String): Notification {
        Log.d("TaskTimerService", "createCountdownNotification: Создание уведомления с обратным отсчетом")

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Задача: $taskName")
            .setContentText("Подзадача: $subtaskName")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setUsesChronometer(true) // Включаем хронограф
            .setChronometerCountDown(true) // Включаем обратный отсчет
            .setWhen(endTime) // Указываем время завершения (в миллисекундах)
            .setOngoing(true)
            .build()
    }

    private fun clearAllNotifications() {
        Log.d("TaskTimerService", "clearAllNotifications: Удаление всех уведомлений")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll() // Удаляем все уведомления
    }


    // --- Будильник ---
    private fun triggerAlarm(priority: Boolean) {
        Log.d("TaskTimerService", "triggerAlarm: Запуск будильника с приоритетом $priority")

        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("ALARM_MESSAGE", "Время вышло!")
            putExtra("priority", priority)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(alarmIntent)
    }

}

// --- Ресивер для будильника ---
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlarmReceiver", "onReceive: Будильник запущен")
        var priority = intent?.getBooleanExtra("priority", false)
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_MESSAGE", "Время вышло!") // Можно передать кастомное сообщение
            putExtra("priority", priority)

            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(alarmIntent)
    }
}


// с - 13 раз
// я - 14 раз
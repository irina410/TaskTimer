package com.example.tasktimer.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R

class TaskTimerService : Service() {
    companion object {
        const val EXTRA_TASK_NUMBER = "task_number" // Добавляем константу
        const val CHANNEL_ID = "task_timer_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_SUBTASKS = "subtasks"
        const val ACTION_STOP_TASK = "STOP_TASK"
    }


    private val activeTimers = mutableMapOf<String, TaskTimer>() // Активные таймеры задач
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateMainNotification() // Обновляем уведомление при каждом тике таймера
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Регистрируем приёмник
        val filter = IntentFilter("com.example.tasktimer.UPDATE_NOTIFICATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(notificationReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(notificationReceiver) // Чистим мусор
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TASK -> stopTask(intent)
            else -> startTask(intent)
        }
        return START_STICKY // Теперь сервис перезапустится при убийстве системы
    }

    override fun onBind(intent: Intent?): IBinder? = null


    // --- Логика запуска задачи ---
    private fun startTask(intent: Intent?) {
        val taskNumber = intent?.getIntExtra(EXTRA_TASK_NUMBER, -1) ?: -1 // Получаем номер
        val taskName = intent?.getStringExtra(EXTRA_TASK_NAME) ?: return
        val subtasks = intent.getParcelableArrayListExtra<Subtask>(EXTRA_SUBTASKS) ?: return

        // Исправленный вызов конструктора TaskTimer
        val taskTimer = TaskTimer(
            taskNumber = taskNumber,
            context = this,
            taskName = taskName,
            subtasks = subtasks,
            notificationManager = notificationManager
        )

        activeTimers[taskName] = taskTimer
        taskTimer.start()
        startForeground(NOTIFICATION_ID, createSilentNotification())
    }

    private fun stopTask(intent: Intent) {
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: return
        val taskNumber = intent.getIntExtra(EXTRA_TASK_NUMBER, -1)

        activeTimers[taskName]?.stop()
        activeTimers.remove(taskName)

        // Очистка данных прогресса
        if (taskNumber != -1) {
            getSharedPreferences("TaskProgress", Context.MODE_PRIVATE).edit().apply {
                remove("task_${taskNumber}_current")
                remove("task_${taskNumber}_remaining")
                remove("task_${taskNumber}_next_desc")
                apply()
            }
        }

        stopSelfIfNoTasks()
    }
    // --- Остановка сервиса, если нет активных задач ---
    private fun stopSelfIfNoTasks() {
        if (activeTimers.isEmpty()) {
            stopSelf()
        }
    }


    // --- Создание канала уведомлений ---
    private fun createNotificationChannel() {
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
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Тихое уведомление (не содержит текста, не вибрирует)
    private fun createSilentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true) // Без звука и вибрации
            .build()
    }

    private fun updateMainNotification() {
        val nextTask = activeTimers.values
            .filter { it.isRunning() }
            .minByOrNull { it.remainingTime() } // Находим ближайшую задачу

        if (nextTask != null) {
            val notification = createNotification(
                nextTask.taskName(),
                nextTask.formatTime(nextTask.remainingTime()),
                nextTask.currentSubtaskName()
            )
            startForeground(NOTIFICATION_ID, notification) // Снова делаем сервис foreground
        }
    }


    // --- Создание уведомления ---
    private fun createNotification(
        taskName: String,
        time: String,
        subtaskName: String
    ): Notification {
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
            .setOngoing(true) // Уведомление нельзя скрыть
            .build()
    }
}

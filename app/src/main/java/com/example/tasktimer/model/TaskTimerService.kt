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

/**
 * Сервис для управления таймерами задач.
 */
class TaskTimerService : Service() {

    companion object {
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
        registerReceiver(notificationReceiver, filter, RECEIVER_EXPORTED)
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

//    override fun onDestroy() {
//        activeTimers.values.forEach { it.stop() } // Останавливаем все таймеры при уничтожении сервиса
//        super.onDestroy()
//    }

    // --- Логика запуска задачи ---
    private fun startTask(intent: Intent?) {
        val taskName = intent?.getStringExtra(EXTRA_TASK_NAME) ?: return
        val subtasks = intent.getParcelableArrayListExtra<Subtask>(EXTRA_SUBTASKS) ?: return

        // Создаем и запускаем таймер для задачи
        val taskTimer = TaskTimer(this, taskName, subtasks, notificationManager)
        activeTimers[taskName] = taskTimer
        taskTimer.start()

        // Запускаем сервис в foreground с тихим уведомлением
        startForeground(NOTIFICATION_ID, createSilentNotification())
    }

    // --- Логика остановки задачи ---
    private fun stopTask(intent: Intent) {
        val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: return
        activeTimers[taskName]?.stop()
        activeTimers.remove(taskName)
        stopSelfIfNoTasks() // Останавливаем сервис, если задач больше нет
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
                nextTask.formatTime(),
                nextTask.currentSubtaskName()
            )
            startForeground(NOTIFICATION_ID, notification) // Снова делаем сервис foreground
        }
        //        else {
//            // НЕ ОСТАНАВЛИВАЕМ сервис, просто обновляем уведомление на "нет активных задач"
//            val emptyNotification = NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("Task Timer")
//                .setContentText("Нет активных задач")
//                .setSmallIcon(R.drawable.ic_timer)
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .build()
//
//            notificationManager.notify(NOTIFICATION_ID, emptyNotification)
//        }
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
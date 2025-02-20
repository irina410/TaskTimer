package com.example.tasktimer.model

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R
import com.example.tasktimer.view.AlarmActivity

/**
 * Класс для управления таймером одной задачи.
 */
class TaskTimer(
    private val context: Context,
    private val taskName: String,
    private val subtasks: List<Subtask>,
    private val notificationManager: NotificationManager
) {
    private var currentSubtaskIndex = 0
    private var currentTimer: CountDownTimer? = null
    private var remainingTime: Long = 0
    private var isRunning = false

    fun start() {
        startSubtaskTimer()
    }

    fun stop() {
        stopCurrentTimer()
        notificationManager.cancel(taskName.hashCode()) // Удаляем уведомление
    }

    // --- Запуск таймера для текущей подзадачи ---
    private fun startSubtaskTimer() {
        if (currentSubtaskIndex >= subtasks.size) {
            Log.d("TaskTimer", "Все подзадачи завершены")
            showCompletionNotification() // Показываем уведомление о завершении задачи
            return
        }
        isRunning = true

        val currentSubtask = subtasks[currentSubtaskIndex]
        remainingTime = currentSubtask.duration * 1000

        stopCurrentTimer()

        // Обновляем уведомление сразу после запуска таймера
        updateNotification(formatTime(remainingTime), currentSubtask.description)

        currentTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateNotification(formatTime(remainingTime), currentSubtask.description)

                // Сообщаем сервису обновить главное уведомление
                val intent = Intent("com.example.tasktimer.UPDATE_NOTIFICATION")
                context.sendBroadcast(intent)
            }

            override fun onFinish() {
                triggerAlarm(currentSubtask.isHighPriority)
                waitForUserConfirmation()

                // Обновляем главное уведомление
                val intent = Intent("com.example.tasktimer.UPDATE_NOTIFICATION")
                context.sendBroadcast(intent)
            }
        }

        currentTimer?.start()
    }

    // --- Показ уведомления о завершении задачи ---
    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(context, TaskTimerService.CHANNEL_ID)
            .setContentTitle("Задача завершена")
            .setContentText("Задача: $taskName завершена")
            .setSmallIcon(R.drawable.ic_timer)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Уведомление можно скрыть
            .build()

        notificationManager.notify(taskName.hashCode(), notification)
    }

    // --- Ожидание подтверждения пользователя ---
    private fun waitForUserConfirmation() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val isConfirmed = intent?.getBooleanExtra("SUBTASK_COMPLETED", false) ?: false
                if (isConfirmed) {
                    currentSubtaskIndex++
                    startSubtaskTimer() // Переходим к следующей подзадаче
                }
                context?.unregisterReceiver(this)
            }
        }

        val filter = IntentFilter("com.example.tasktimer.SUBTASK_COMPLETED")
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    }

    // --- Запуск будильника ---
    private fun triggerAlarm(isHighPriority: Boolean) {
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_MESSAGE", "Время вышло!")
            putExtra("priority", isHighPriority)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(alarmIntent)
    }

    // --- Остановка текущего таймера ---
    private fun stopCurrentTimer() {
        currentTimer?.cancel()
        isRunning = false
    }

    // --- Обновление уведомления ---
    private fun updateNotification(time: String, subtaskName: String) {
        val notification = createNotification(time, subtaskName)
        notificationManager.notify(taskName.hashCode(), notification)
    }

    // --- Создание уведомления ---
    private fun createNotification(time: String, subtaskName: String): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, TaskTimerService.CHANNEL_ID)
            .setContentTitle("Задача: $taskName")
            .setContentText("Подзадача: $subtaskName — $time")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)// Уведомление нельзя скрыть
            .setSilent(true)
            .build()
    }

    // --- Форматирование времени в HH:MM:SS ---
    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    fun isRunning(): Boolean = isRunning
    fun remainingTime(): Long = remainingTime
    fun taskName(): String = taskName
    fun formatTime(): String = formatTime(remainingTime)
    fun currentSubtaskName(): String = subtasks.getOrNull(currentSubtaskIndex)?.description ?: "Неизвестная подзадача"
}
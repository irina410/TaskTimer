package com.example.tasktimer.model

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R

class TaskTimerService : Service() {

    companion object {
        // Константы для уведомлений и данных
        const val CHANNEL_ID = "task_timer_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_SUBTASK_NAME = "subtask_name"
        const val BROADCAST_UPDATE = "task_timer_update"
        const val EXTRA_CURRENT_SUBTASK = "current_subtask"
        const val EXTRA_REMAINING_TIME = "remaining_time"
    }

    // Переменные для состояния таймера
    private var remainingTime: Long = 0
    private var startTime = 0L
    private var taskName: String? = null
    private var subtaskName: String? = null
    private var currentSubtaskIndex = 0
    private var subtasks: List<Subtask> = emptyList()
    private var totalTime: Long = 0
    private var isRunning = false
    private var currentTimer: CountDownTimer? = null

    // Объекты для работы с уведомлениями и обновлениями
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 1000 // Интервал обновления уведомлений в миллисекундах

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // Создаем канал уведомлений
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Извлекаем данные из intent
        taskName = intent?.getStringExtra(EXTRA_TASK_NAME)
        subtaskName = intent?.getStringExtra(EXTRA_SUBTASK_NAME)
        subtasks = intent?.getParcelableArrayListExtra("subtasks") ?: emptyList()
        totalTime = intent?.getLongExtra("totalTime", 0) ?: 0
        startTime = System.currentTimeMillis()

        // Загружаем состояние задачи
        currentSubtaskIndex = loadCurrentSubtaskIndex()
        remainingTime = loadRemainingTime()

        // Запускаем уведомление и таймер
        startForeground(NOTIFICATION_ID, createNotification("00:00:00"))
        updateNotification()

        if (subtasks.isNotEmpty()) {
            startSubtaskTimer()
        }

        return START_NOT_STICKY
    }

    /**
     * Метод для запуска таймера для текущей подзадачи.
     * Если подзадачи завершены, сервис останавливается.
     */
    private fun startSubtaskTimer() {
        if (currentSubtaskIndex >= subtasks.size) {
            stopSelf() // Все подзадачи завершены
            return
        }

        val subtask = subtasks[currentSubtaskIndex]
        val duration = if (remainingTime > 0) remainingTime else subtask.duration * 1000

        // Создаем и запускаем CountDownTimer
        currentTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                sendUpdateBroadcast(subtask.description, millisUntilFinished / 1000)
            }

            override fun onFinish() {
                currentSubtaskIndex++
                saveCurrentSubtaskIndex(currentSubtaskIndex)
                saveRemainingTime(0)
                startSubtaskTimer()
            }
        }.start()

        // Сохраняем оставшееся время
        saveRemainingTime(duration)
    }

    /**
     * Отправляет локальное обновление через BroadcastManager.
     */
    private fun sendUpdateBroadcast(subtaskName: String, remainingSeconds: Long) {
        val intent = Intent(BROADCAST_UPDATE).apply {
            putExtra(EXTRA_CURRENT_SUBTASK, subtaskName)
            putExtra(EXTRA_REMAINING_TIME, remainingSeconds)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    /**
     * Сохраняет текущий индекс подзадачи в SharedPreferences.
     */
    private fun saveCurrentSubtaskIndex(index: Int) {
        getSharedPreferences("TaskTimerPrefs", MODE_PRIVATE).edit()
            .putInt("currentSubtaskIndex", index).apply()
    }

    /**
     * Загружает текущий индекс подзадачи из SharedPreferences.
     */
    private fun loadCurrentSubtaskIndex(): Int {
        return getSharedPreferences("TaskTimerPrefs", MODE_PRIVATE)
            .getInt("currentSubtaskIndex", 0)
    }

    /**
     * Сохраняет оставшееся время в SharedPreferences.
     */
    private fun saveRemainingTime(time: Long) {
        getSharedPreferences("TaskTimerPrefs", MODE_PRIVATE).edit()
            .putLong("remainingTime", time).apply()
    }

    /**
     * Загружает оставшееся время из SharedPreferences.
     */
    private fun loadRemainingTime(): Long {
        return getSharedPreferences("TaskTimerPrefs", MODE_PRIVATE)
            .getLong("remainingTime", 0)
    }

    /**
     * Создает уведомление с текущим временем.
     */
    private fun createNotification(time: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(taskName ?: "Текущая задача")
            .setContentText("Подзадача: ${subtaskName ?: "Не указана"} — $time")
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * Обновляет уведомление каждую секунду.
     */
    private fun updateNotification() {
        handler.postDelayed({
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            val time = formatElapsedTime(elapsedSeconds)
            val notification = createNotification(time)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
            updateNotification()
        }, updateInterval)
    }

    /**
     * Форматирует время в формате HH:mm:ss.
     */
    private fun formatElapsedTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    /**
     * Создает канал уведомлений для Android 8.0 и выше.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Timer notifications for tasks"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        currentTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


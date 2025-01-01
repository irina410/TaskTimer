package com.example.tasktimer.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R

class TaskTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "task_timer_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_SUBTASK_NAME = "subtask_name"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var taskName: String? = null
    private var subtaskName: String? = null
    private val updateInterval: Long = 1000 // Обновление каждую секунду

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        taskName = intent?.getStringExtra(EXTRA_TASK_NAME)
        subtaskName = intent?.getStringExtra(EXTRA_SUBTASK_NAME)
        startTime = System.currentTimeMillis()

        startForeground(NOTIFICATION_ID, createNotification("00:00:00"))
        updateNotification()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Timer notifications for tasks"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

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

    private fun updateNotification() {
        handler.postDelayed({
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            val time = formatElapsedTime(elapsedSeconds)
            val notification = createNotification(time)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
            updateNotification() // Рекурсивно вызываем для обновления
        }, updateInterval)
    }

    private fun formatElapsedTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}

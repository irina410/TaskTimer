package com.example.tasktimer.model

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R

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
    }

    private fun startSubtaskTimer() {
        if (currentSubtaskIndex >= subtasks.size) {
            Log.d("TaskTimer", "Все подзадачи завершены")
            return
        }
        isRunning = true

        val currentSubtask = subtasks[currentSubtaskIndex]
        remainingTime = currentSubtask.duration * 1000

        stopCurrentTimer()

        currentTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                val formattedTime = formatTime(remainingTime)
                updateNotification(formattedTime, currentSubtask.description)
                Log.d("TaskTimer", "Осталось времени: $formattedTime")
            }

            override fun onFinish() {
                Log.d("TaskTimer", "Таймер подзадачи #$currentSubtaskIndex завершён")
                currentSubtaskIndex++
                startSubtaskTimer()
            }
        }

        currentTimer?.start()
    }

    private fun stopCurrentTimer() {
        currentTimer?.cancel()
        isRunning = false
    }

    private fun updateNotification(time: String, subtaskName: String) {
        val notification = createNotification(time, subtaskName)
        notificationManager.notify(taskName.hashCode(), notification)
    }

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
            .setOngoing(true)
            .build()
    }

    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
package com.example.tasktimer.model

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.tasktimer.MainActivity
import com.example.tasktimer.R
import com.example.tasktimer.view.AlarmActivity

class TaskTimer(
    private val taskNumber: Int,
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
        notificationManager.cancel(taskName.hashCode())
    }

    private fun startSubtaskTimer() {
        if (currentSubtaskIndex >= subtasks.size) {
            Log.d("TaskTimer", "Все подзадачи завершены")
            showCompletionNotification()
            context.sendBroadcast(Intent("com.example.tasktimer.TASK_COMPLETED").apply {
                putExtra("TASK_NUMBER", taskNumber)
            })
            return
        }
        isRunning = true

        val currentSubtask = subtasks[currentSubtaskIndex]
        remainingTime = currentSubtask.duration * 1000L

        stopCurrentTimer()

        updateNotification(formatTime(remainingTime), currentSubtask.description)

        currentTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished
                updateNotification(formatTime(remainingTime), currentSubtask.description)
                saveProgress()
                context.sendBroadcast(Intent("com.example.tasktimer.UPDATE_NOTIFICATION"))
            }

            override fun onFinish() {
                triggerAlarm(currentSubtask)
                waitForUserConfirmation()
                context.sendBroadcast(Intent("com.example.tasktimer.UPDATE_NOTIFICATION"))
            }
        }.start()
    }

    private fun showCompletionNotification() {
        // Очистка данных прогресса
        context.getSharedPreferences("TaskProgress", Context.MODE_PRIVATE).edit().apply {
            remove("task_${taskNumber}_current")
            remove("task_${taskNumber}_remaining")
            remove("task_${taskNumber}_next_desc")
            apply()
        }

        NotificationCompat.Builder(context, TaskTimerService.CHANNEL_ID)
            .setContentTitle("Задача завершена")
            .setContentText("Задача: $taskName завершена")
            .setSmallIcon(R.drawable.ic_timer)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            .let { notificationManager.notify(taskName.hashCode(), it) }
    }

    private fun waitForUserConfirmation() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getBooleanExtra("SUBTASK_COMPLETED", false) == true) {
                    currentSubtaskIndex++
                    startSubtaskTimer()
                }
                context?.unregisterReceiver(this)
            }
        }

        val filter = IntentFilter("com.example.tasktimer.SUBTASK_COMPLETED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    private fun triggerAlarm(currentSubtask: Subtask) {
        context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE).edit().apply {
            val nextIndex = currentSubtaskIndex + 1
            putString("TASK_NAME", taskName)
            putString("COMPLETED_SUBTASK", currentSubtask.description)
            putString("COMPLETED_TIME", formatTime(currentSubtask.duration * 1000L))
            putString("NEXT_SUBTASK", subtasks.getOrNull(nextIndex)?.description ?: "Все подзадачи выполнены!")
            putBoolean("PRIORITY", currentSubtask.isHighPriority)
            putBoolean("NEXT_PR", subtasks.getOrNull(nextIndex)?.isHighPriority ?: false)
            putLong("NEXT_TIME", subtasks.getOrNull(nextIndex)?.duration ?: 0)

            apply()
        }

        context.startActivity(
            Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun stopCurrentTimer() {
        currentTimer?.cancel()
        isRunning = false
    }

    private fun updateNotification(time: String, subtaskName: String) {
        notificationManager.notify(
            taskName.hashCode(),
            NotificationCompat.Builder(context, TaskTimerService.CHANNEL_ID)
                .setContentTitle("Задача: $taskName")
                .setContentText("Подзадача: $subtaskName — $time")
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setOngoing(true)
                .setSilent(true)
                .build()
        )
    }

    fun formatTime(milliseconds: Long): String {
        return "%02d:%02d:%02d".format(
            milliseconds / 3600000,
            (milliseconds % 3600000) / 60000,
            (milliseconds % 60000) / 1000
        )
    }

    private fun saveProgress() {
        context.getSharedPreferences("TaskProgress", Context.MODE_PRIVATE).edit().apply {
            val nextIndex = currentSubtaskIndex + 1
            putInt("task_${taskNumber}_current", currentSubtaskIndex)
            putLong("task_${taskNumber}_remaining", remainingTime)
            putString("task_${taskNumber}_next_desc", subtasks.getOrNull(nextIndex)?.description)
            apply()
        }
    }

    // Getters для сервиса
    fun isRunning() = isRunning
    fun remainingTime() = remainingTime
    fun taskName() = taskName
    fun currentSubtaskName() = subtasks.getOrNull(currentSubtaskIndex)?.description ?: "N/A"
}
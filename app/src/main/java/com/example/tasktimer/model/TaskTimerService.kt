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

    private var currentTimer: CountDownTimer? = null
    private var isRunning = false
    private var currentSubtaskIndex = 0
    private var subtasks: List<Subtask> = emptyList()
    private var totalTime: Long = 0

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

        // Получаем подзадачи и общее время из intent
        subtasks = intent?.getParcelableArrayListExtra("subtasks") ?: emptyList()
        totalTime = intent?.getLongExtra("totalTime", 0) ?: 0

        // Стартуем таймер, если есть подзадачи
        if (subtasks.isNotEmpty()) {
            startAlgorithmTimer(subtasks, totalTime)
        }

        return START_NOT_STICKY
    }

    private fun startAlgorithmTimer(subtasks: List<Subtask>, totalTime: Long) {
        if (subtasks.isEmpty()) return

        val subtask = subtasks[currentSubtaskIndex]

        currentTimer = object : CountDownTimer(subtask.duration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Обновляем уведомление с оставшимся временем
                val time = formatElapsedTime(millisUntilFinished / 1000)
                val notification = createNotification(time)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }

            override fun onFinish() {
                triggerAlarmAndWait(subtask.description, subtask.duration, subtasks, totalTime)
            }
        }.start()

        isRunning = true
    }

    private fun triggerAlarmAndWait(
        subtaskName: String,
        duration: Long,
        subtasks: List<Subtask>,
        totalTime: Long
    ) {
        val subtask = subtasks[currentSubtaskIndex]
        val ringtoneUri = if (subtask.isHighPriority) {
            Uri.parse("android.resource://${packageName}/raw/electronic_alarm_signal")
        } else {
            Uri.parse("android.resource://${packageName}/raw/basic_alarm_ringtone")
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val desiredVolume = (maxVolume * 0.7).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, desiredVolume, 0)

        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
        ringtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        ringtone.play()

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
            vibrator.vibrate(vibrationEffect)
        } else {
            @Suppress("DEPRECATION") // Suppress deprecation warning for older API levels
            vibrator.vibrate(longArrayOf(0, 500, 500), 0)        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            vibrator.vibrate(vibrationEffect)
//        } else {
//            vibrator.vibrate(longArrayOf(0, 500, 500), 0)
//        }

        // Показать кастомное диалоговое окно
        showAlarmDialog(
            subtaskName,
            duration,
            subtasks,
            totalTime,
            ringtone,
            vibrator,
            originalVolume
        )
    }

    private fun showAlarmDialog(
        subtaskName: String,
        duration: Long,
        subtasks: List<Subtask>,
        totalTime: Long,
        ringtone: Ringtone,
        vibrator: Vibrator,
        originalVolume: Int
    ) {
        val windowContext = applicationContext
        val layoutInflater = LayoutInflater.from(windowContext)
        val dialogView = layoutInflater.inflate(R.layout.dialog_alarm, null)
        val currentSubtaskText = "$subtaskName завершена за ${formatElapsedTime(duration)}" +
                if (subtasks[currentSubtaskIndex].isHighPriority) "\n(Высокий приоритет)" else ""
        val nextSubtask = if (currentSubtaskIndex + 1 < subtasks.size) {
            "Следующая подзадача: ${subtasks[currentSubtaskIndex + 1].description}"
        } else {
            "Это была последняя подзадача!"
        }
        dialogView.findViewById<TextView>(R.id.dialog_message).text =
            "$currentSubtaskText\n\n$nextSubtask"

        val alertDialog = AlertDialog.Builder(windowContext)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.window?.apply {
            setType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE
            )
            addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        alertDialog.show()

        dialogView.findViewById<Button>(R.id.dialog_button).setOnClickListener {
            ringtone.stop()
            vibrator.cancel()
            alertDialog.dismiss()

            // Восстанавливаем громкость
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)

            currentSubtaskIndex++
            if (currentSubtaskIndex < subtasks.size) {
                startAlgorithmTimer(subtasks, totalTime)
            } else {
                completeAlgorithm()
            }
        }
    }

    private fun completeAlgorithm() {
        Toast.makeText(applicationContext, "Все подзадачи завершены!", Toast.LENGTH_SHORT).show()
        stopForeground(true)
        stopSelf()
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
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


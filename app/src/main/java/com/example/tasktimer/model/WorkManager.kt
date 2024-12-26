package com.example.tasktimer.model

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*

class AlarmWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        // Здесь выполняется уведомление или другая задача
        val title = inputData.getString("title") ?: "Будильник"
        val message = inputData.getString("message") ?: "Время завершено!"

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, "alarm_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(0, notification)

        return Result.success()
    }
}

// Планирование задачи
fun scheduleWork(context: Context, delayMillis: Long, title: String, message: String) {
    val inputData = workDataOf("title" to title, "message" to message)

    val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
        .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
        .setInputData(inputData)
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}

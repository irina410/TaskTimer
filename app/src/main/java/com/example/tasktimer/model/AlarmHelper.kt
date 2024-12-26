package com.example.tasktimer.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.tasktimer.view.AlarmActivity

class AlarmHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Метод для создания PendingIntent
    private fun getAlarmActionPendingIntent(requestCode: Int, message: String): PendingIntent {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_MESSAGE", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun setAlarm(timeInMillis: Long, requestCode: Int, message: String) {
        acquireWakeLock()
        val actionPendingIntent = getAlarmActionPendingIntent(requestCode, message)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, actionPendingIntent)

        alarmManager.setAlarmClock(alarmClockInfo, actionPendingIntent)
    }

    private fun acquireWakeLock() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskTimer::AlarmWakeLock"
        )
        wakeLock.acquire(10 * 60 * 1000L /*10 минут*/)
    }
}


package com.example.tasktimer.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tasktimer.R

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alarm_window)
        supportActionBar?.hide()


        val sharedPrefs = getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)

        val taskNumber = sharedPrefs.getInt("TASK_NUMBER", -1)
        val taskName = sharedPrefs.getString("TASK_NAME", "Без названия")
        val totalTime = sharedPrefs.getString("TOTAL_TIME", "Не указано")
        val completedSubtask = sharedPrefs.getString("COMPLETED_SUBTASK", "Нет данных")
        val completedTime = sharedPrefs.getString("COMPLETED_TIME", "?")
        val nextSubtask = sharedPrefs.getString("NEXT_SUBTASK", null)
        val nexttime = formatTime(sharedPrefs.getLong("NEXT_TIME", 0)*1000L)
        val nextprior = sharedPrefs.getBoolean("NEXT_PR", false)
        val priority = sharedPrefs.getBoolean("PRIORITY", false)


        findViewById<TextView>(R.id.task_title).text = "$taskNumber $taskName ($totalTime)"


        val completedTextView = findViewById<TextView>(R.id.completed_subtask)
        val completedPrefix = "Завершено: "
        val completedSpannable = SpannableString("$completedPrefix$completedSubtask ($completedTime)").apply {
            if (completedSubtask != null) {
                setSpan(StyleSpan(Typeface.BOLD), completedPrefix.length, completedPrefix.length + completedSubtask.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        completedTextView.text = completedSpannable


        val nextTextView = findViewById<TextView>(R.id.next_subtask)
        val nextPrefix = "Следующее: "
        if (nextSubtask.isNullOrEmpty() || nextSubtask == "Нет данных") {
            nextTextView.text = "Все подзадачи выполнены!"
        } else {
            val nextTimeText = if (nexttime == "00:00:00") "" else " ($nexttime)"
            val nextSpannable = SpannableString("$nextPrefix$nextSubtask$nextTimeText").apply {
                setSpan(StyleSpan(Typeface.BOLD), nextPrefix.length, nextPrefix.length + nextSubtask.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (nextTimeText.isNotEmpty()) {
                    setSpan(StyleSpan(Typeface.BOLD), nextPrefix.length + nextSubtask.length, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            nextTextView.text = nextSpannable



            if (nextprior) {
                nextTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
            } else {
                nextTextView.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
            }
        }

        val priorityTextView = findViewById<TextView>(R.id.priority_text)
        if (priority) {
            priorityTextView.text = "Высокий приоритет!"
            priorityTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
            priorityTextView.visibility = View.VISIBLE
        } else {
            priorityTextView.visibility = View.GONE
        }


        val dismissButton = findViewById<Button>(R.id.dismiss_button)
        dismissButton.setOnClickListener {
            stopAlarmSound()
            stopVibration()

            val intent = Intent("com.example.tasktimer.SUBTASK_COMPLETED").apply {
                putExtra("SUBTASK_COMPLETED", true)
            }
            sendBroadcast(intent)

            finish()
        }

        playAlarmSound(priority)
        startVibration()


    }
    private fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / 3600000
        val minutes = (milliseconds % 3600000) / 60000
        val seconds = (milliseconds % 60000) / 1000
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    private fun playAlarmSound(priority: Boolean) {
        setAlarmVolume(2)
        val ringtoneResId = when (priority) {
            false -> {
                R.raw.basic_alarm_ringtone
            }
            true -> {
                R.raw.electronic_alarm_signal
            }
        }

        try {
            val afd = resources.openRawResourceFd(ringtoneResId)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmActivity", "playAlarmSound: Ошибка при воспроизведении звука: ${e.message}")
        }
    }

    private fun setAlarmVolume(volumeLevel: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        val safeVolumeLevel = volumeLevel.coerceIn(0, maxVolume)

        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, safeVolumeLevel, 0)

    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun startVibration() {

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
        } else {
            null
        }

        if (vibrationEffect != null) {
            vibrator?.vibrate(vibrationEffect)
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying || it.isLooping) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: IllegalStateException) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
    }
}
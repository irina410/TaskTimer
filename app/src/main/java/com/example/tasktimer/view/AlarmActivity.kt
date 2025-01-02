package com.example.tasktimer.view

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasktimer.R

class AlarmActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alarm_window)

        // Получение сообщения из Intent
        val message = intent.getStringExtra("ALARM_MESSAGE") ?: "Время вышло!"

        // Установка текста сообщения
        findViewById<TextView>(R.id.alarm_message).text = message

        // Настройка воспроизведения мелодии
        playAlarmSound()

        // Включение вибрации
        startVibration()

        // Кнопка для остановки будильника
        findViewById<View>(R.id.dismiss_button).setOnClickListener {
            stopAlarmSound()
            stopVibration()
            finish()
        }
    }

    private fun playAlarmSound() {
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(this@AlarmActivity, defaultUri)
            setAudioStreamType(AudioManager.STREAM_ALARM) // Установка потока
            isLooping = true
            prepare()
            start()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0) // Вибрация: пауза 0, вибрация 500мс, пауза 500мс, бесконечно
        } else {
            null
        }

        if (vibrationEffect != null) {
            vibrator?.vibrate(vibrationEffect)
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0) // Для старых устройств
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun stopAlarmSound() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
    }
}

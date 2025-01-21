package com.example.tasktimer.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasktimer.R

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alarm_window)

        Log.d("AlarmActivity", "onCreate: Создание активности будильника")

        // Получение сообщения и приоритета из Intent
        val message = intent.getStringExtra("ALARM_MESSAGE") ?: "Время вышло!"
        val priority = intent.getBooleanExtra("priority", false)

        Log.d("AlarmActivity", "onCreate: Сообщение: $message, Приоритет: $priority")

        // Установка текста сообщения
        findViewById<TextView>(R.id.alarm_message).text = message

        // Настройка звука и вибрации
        playAlarmSound(priority)
        startVibration()

        // Кнопка для остановки будильника
        findViewById<View>(R.id.dismiss_button).setOnClickListener {
            Log.d("AlarmActivity", "Кнопка 'Готово' нажата")
            stopAlarmSound()
            stopVibration()

            // Отправляем Broadcast о завершении подзадачи
            val intent = Intent("com.example.tasktimer.SUBTASK_COMPLETED").apply {
                putExtra("SUBTASK_COMPLETED", true)
            }
            sendBroadcast(intent)

            finish()
        }


    }

    private fun playAlarmSound(priority: Boolean) {
        Log.d("AlarmActivity", "playAlarmSound: Воспроизведение звука будильника")
        setAlarmVolume(0)
        val ringtoneResId = when (priority) {
            false -> {
                Log.d("AlarmActivity", "playAlarmSound: Using basic_alarm_ringtone $priority")
                //R.raw.basic_alarm_ringtone
            }
            true -> {
                Log.d("AlarmActivity", "playAlarmSound: Using electronic_alarm_signal $priority")
                //R.raw.electronic_alarm_signal
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
            Log.d("AlarmActivity", "playAlarmSound: Звук будильника начал воспроизводиться")
        } catch (e: Exception) {
            Log.e("AlarmActivity", "playAlarmSound: Ошибка при воспроизведении звука: ${e.message}")
        }
    }


    private fun setAlarmVolume(volumeLevel: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Получаем максимальную громкость для потока будильника
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

        // Проверяем, что уровень громкости находится в допустимых пределах
        val safeVolumeLevel = volumeLevel.coerceIn(0, maxVolume)

        // Устанавливаем громкость
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, safeVolumeLevel, 0)

        Log.d("AlarmActivity", "setAlarmVolume: Громкость будильника установлена на $safeVolumeLevel из $maxVolume")
    }



    @SuppressLint("MissingPermission", "NewApi")
    private fun startVibration() {
        Log.d("AlarmActivity", "startVibration: Включение вибрации")

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0) // Вибрация с паузой
        } else {
            null
        }

        if (vibrationEffect != null) {
            vibrator?.vibrate(vibrationEffect)
            Log.d("AlarmActivity", "startVibration: Вибрация с эффектом для новых устройств")
        } else {
            vibrator?.vibrate(longArrayOf(0, 500, 500), 0) // Для старых устройств
            Log.d("AlarmActivity", "startVibration: Вибрация для старых устройств")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopVibration() {
        Log.d("AlarmActivity", "stopVibration: Остановка вибрации")
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
            Log.e("AlarmActivity", "Error stopping alarm sound: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmActivity", "onDestroy: Активность уничтожена")
        stopAlarmSound()
        stopVibration()
    }
}

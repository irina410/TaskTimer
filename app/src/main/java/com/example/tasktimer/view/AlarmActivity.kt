package com.example.tasktimer.view

import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tasktimer.R


class AlarmActivity : AppCompatActivity() {

    private lateinit var ringtone: Ringtone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alarm_window)

        // Получение сообщения из Intent
        val message = intent.getStringExtra("ALARM_MESSAGE") ?: "Время вышло!"

        // Установка текста сообщения
        findViewById<TextView>(R.id.alarm_message).text = message

        // Использование мелодии по умолчанию
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, defaultUri)
        ringtone.play()

        // Кнопка для остановки будильника
        findViewById<View>(R.id.dismiss_button).setOnClickListener {
            ringtone.stop()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ringtone.isPlaying) {
            ringtone.stop()
        }
    }
}

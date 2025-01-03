import android.app.AlertDialog
import android.os.CountDownTimer
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.NotificationChannel
import android.media.RingtoneManager
import android.media.Ringtone
import android.content.Intent
import android.app.Notification
import android.content.Context
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Subtask
import com.example.tasktimer.model.Task
import com.example.tasktimer.model.TaskTimerService
import com.example.tasktimer.view.AlarmActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onTaskDelete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task, onTaskDelete)

    }

    override fun getItemCount(): Int = tasks.size

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNumber: TextView = itemView.findViewById(R.id.taskNumber)
        private val algorithmName: TextView = itemView.findViewById(R.id.taskName)
        private val taskTime: TextView = itemView.findViewById(R.id.taskTime)
        private val startStopButton: FloatingActionButton = itemView.findViewById(R.id.startStopButton)
        private var currentTimer: CountDownTimer? = null
        private var isRunning = false
        private var currentSubtaskIndex = 0
        private val notificationManager =
            itemView.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        private val channelId = "TASK_TIMER_CHANNEL"
        private val subtaskLayout: LinearLayout = itemView.findViewById(R.id.subtaskLayout)
        private val subtaskCountdown: TextView = itemView.findViewById(R.id.subtaskCountdown)
        private val currentSubtask: TextView = itemView.findViewById(R.id.currentSubtask)

        init {
            createNotificationChannel()
        }


        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            taskNumber.text = task.number.toString()
            algorithmName.text = task.algorithm.name
            taskTime.text = formatTime(task.algorithm.totalTime)


            updateButtonIcon(isRunning)

            startStopButton.setOnClickListener {
                val serviceIntent = Intent(itemView.context, TaskTimerService::class.java)
                serviceIntent.putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                serviceIntent.putExtra(TaskTimerService.EXTRA_SUBTASK_NAME, task.algorithm.subtasks[0].description) // Assuming the first subtask is started initially
                serviceIntent.putParcelableArrayListExtra("subtasks", ArrayList(task.algorithm.subtasks))
                serviceIntent.putExtra("totalTime", task.algorithm.totalTime)

                if (isRunning) {
                    itemView.context.stopService(serviceIntent) // Stop the service
                    stopAlgorithmTimer() // Update UI
                } else {
                    itemView.context.startService(serviceIntent) // Start the service
                    startAlgorithmTimer(task.algorithm.subtasks, task.algorithm.totalTime) // Update UI
                }
            }

            itemView.setOnLongClickListener {
                showDeleteConfirmationDialog(itemView.context, task, onTaskDelete)
                true
            }
        }

        private fun showDeleteConfirmationDialog(
            context: Context,
            task: Task,
            onTaskDelete: (Task) -> Unit
        ) {
            AlertDialog.Builder(context)
                .setTitle("Удалить задачу?")
                .setMessage("Вы точно хотите удалить задачу №${task.number} (${task.algorithm.name})?")
                .setPositiveButton("Удалить") { _, _ -> onTaskDelete(task) }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun startAlgorithmTimer(subtasks: List<Subtask>, totalTime: Long) {
            // Update UI elements to reflect the running state
            subtaskLayout.visibility = View.VISIBLE
            currentSubtask.text = "Текущая подзадача: ${subtasks[currentSubtaskIndex].description}"
            subtaskCountdown.text = "Оставшееся время: ${formatTime(subtasks[currentSubtaskIndex].duration)}" // Initial time
            isRunning = true
            updateButtonIcon(true)
        }


        private fun triggerAlarmAndWait(
            subtaskName: String,
            duration: Long,
            subtasks: List<Subtask>,
            totalTime: Long
        ) {
            val subtask = subtasks[currentSubtaskIndex]

            val ringtoneUri = if (subtask.isHighPriority) {
                Uri.parse("android.resource://${itemView.context.packageName}/raw/electronic_alarm_signal") // Уникальный звук для high-priority
            } else {
                Uri.parse("android.resource://${itemView.context.packageName}/raw/basic_alarm_ringtone") // Стандартный звук
            }

            val audioManager = itemView.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val desiredVolume = (maxVolume * 0.7).toInt() // Устанавливаем 80% от максимальной громкости
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                desiredVolume,
                0
            )

            val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val ringtone = RingtoneManager.getRingtone(itemView.context, ringtoneUri)
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            ringtone.play()

            // Включаем вибрацию
            val vibrator = itemView.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            val vibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0) // Вибрация: 500 мс, пауза 500 мс, бесконечно
            } else {
                longArrayOf(0, 500, 500)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 500), // Задержка, вибрация, пауза
                    0 // Повтор (0 - бесконечно, -1 - не повторять)
                )
                vibrator.vibrate(vibrationEffect)
            } else {
                // Для старых версий Android используем миллисекунды
                vibrator.vibrate(longArrayOf(0, 500, 500), 0)
            }

            val windowContext = itemView.context.applicationContext
            val layoutInflater = LayoutInflater.from(windowContext)

            // Создаём кастомное окно
            val dialogView = layoutInflater.inflate(R.layout.dialog_alarm, null)
            val currentSubtaskText = "$subtaskName завершена за ${formatTime(duration)}" +
                    if (subtask.isHighPriority) "\n(Высокий приоритет)" else ""
            val nextSubtask = if (currentSubtaskIndex + 1 < subtasks.size) {
                "Следующая подзадача: ${subtasks[currentSubtaskIndex + 1].description}"
            } else {
                "Это была последняя подзадача!"
            }
            dialogView.findViewById<TextView>(R.id.dialog_message).text = "$currentSubtaskText\n\n$nextSubtask"

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
                vibrator.cancel() // Останавливаем вибрацию
                alertDialog.dismiss()

                // Восстанавливаем громкость
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)

                currentSubtaskIndex++
                if (currentSubtaskIndex < subtasks.size) {
                    startAlgorithmTimer(subtasks, totalTime)
                } else {
                    completeAlgorithm()
                }
            }
        }




        private fun showFullScreenNotification(subtaskName: String) {
            val intent = Intent(itemView.context, AlarmActivity::class.java).apply {
                putExtra("SUBTASK_NAME", subtaskName)
            }
            val pendingIntent = PendingIntent.getActivity(
                itemView.context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(itemView.context, channelId)
                .setContentTitle("Подзадача завершена")
                .setContentText("$subtaskName завершена")
                .setSmallIcon(R.drawable.ic_timer)
                .setFullScreenIntent(pendingIntent, true) // Важная часть для отображения на экране блокировки
                .setAutoCancel(true)
                .build()

            notificationManager.notify(2, notification)
        }


        private fun stopAlgorithmTimer() {
            // Update UI elements to reflect the stopped state
            subtaskLayout.visibility = View.GONE
            isRunning = false
            updateButtonIcon(false)
            currentSubtaskIndex = 0 // Reset subtask index
        }


        private fun updateButtonIcon(isRunning: Boolean) {
            startStopButton.setImageResource(
                if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }

        private fun showProgressNotification(subtaskName: String, duration: Long) {
            val notification = NotificationCompat.Builder(itemView.context, channelId)
                .setContentTitle("Выполняется подзадача")
                .setContentText("$subtaskName: ${formatTime(duration)}")
                .setSmallIcon(R.drawable.ic_timer)
                .setOngoing(true)
                .build()
            notificationManager.notify(1, notification)
        }

        private fun updateNotification(subtaskName: String, millisUntilFinished: Long) {
            val notification = NotificationCompat.Builder(itemView.context, channelId)
                .setContentTitle("Выполняется подзадача")
                .setContentText("$subtaskName: ${formatTime(millisUntilFinished / 1000)}")
                .setSmallIcon(R.drawable.ic_timer)
                .setOngoing(true)
                .build()
            notificationManager.notify(1, notification)
        }

        private fun triggerAlarm(subtaskName: String, duration: Long) {
            val ringtone =
                RingtoneManager.getRingtone(itemView.context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ringtone.play()

            AlertDialog.Builder(itemView.context)
                .setTitle("Подзадача завершена")
                .setMessage("$subtaskName завершена за ${formatTime(duration)}")
                .setPositiveButton("Готово") { _, _ -> ringtone.stop() }
                .show()

            removeNotification()
        }

        private fun removeNotification() {
            notificationManager.cancel(1)
        }

        private fun completeAlgorithm() {
            Toast.makeText(itemView.context, "Все подзадачи завершены!", Toast.LENGTH_SHORT).show()
            subtaskLayout.visibility = View.GONE
            stopAlgorithmTimer()
        }


        private fun createNotificationChannel() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Task Timer",
                    NotificationManager.IMPORTANCE_LOW
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}


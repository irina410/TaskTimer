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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Subtask
import com.example.tasktimer.model.Task
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

        init {
            createNotificationChannel()
        }


        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            taskNumber.text = task.number.toString()
            algorithmName.text = task.algorithm.name
            taskTime.text = formatTime(task.algorithm.totalTime)

            updateButtonIcon(isRunning)

            startStopButton.setOnClickListener {
                if (isRunning) {
                    stopAlgorithmTimer()
                } else {
                    startAlgorithmTimer(task.algorithm.subtasks, task.algorithm.totalTime)
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
            if (subtasks.isEmpty()) return
            updateButtonIcon(true)

            val subtask = subtasks[currentSubtaskIndex]
            showProgressNotification(subtask.description, subtask.duration)

            currentTimer = object : CountDownTimer(subtask.duration * 1000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    updateNotification(subtask.description, millisUntilFinished)
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
            val ringtone = RingtoneManager.getRingtone(itemView.context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            ringtone.play()

            AlertDialog.Builder(itemView.context)
                .setTitle("Подзадача завершена")
                .setMessage("$subtaskName завершена за ${formatTime(duration)}")
                .setPositiveButton("Готово") { _, _ ->
                    ringtone.stop()
                    currentSubtaskIndex++
                    if (currentSubtaskIndex < subtasks.size) {
                        startAlgorithmTimer(subtasks, totalTime) // Запуск следующей подзадачи
                    } else {
                        completeAlgorithm() // Все подзадачи завершены
                    }
                }
                .setCancelable(false) // Запретить закрытие диалога без выбора
                .show()

            removeNotification() // Убираем уведомление
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
            currentTimer?.cancel()
            removeNotification()
            updateButtonIcon(false)
            isRunning = false
            currentSubtaskIndex = 0
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


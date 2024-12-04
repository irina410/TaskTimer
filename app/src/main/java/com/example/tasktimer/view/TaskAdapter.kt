import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Subtask
import com.example.tasktimer.model.Task
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
    private fun showDeleteConfirmationDialog(context: Context, task: Task, onTaskDelete: (Task) -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Удалить задачу?")
            .setMessage("Ты точно хочешь удалить эту задачу? Это действие необратимо!")
            .setPositiveButton("Да") { _, _ ->
                onTaskDelete(task)
            }
            .setNegativeButton("Нет", null)
            .show()
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
                    triggerAlarm(subtask.description, subtask.duration)
                    currentSubtaskIndex++
                    if (currentSubtaskIndex < subtasks.size) {
                        startAlgorithmTimer(subtasks, totalTime)
                    } else {
                        completeAlgorithm()
                    }
                }
            }.start()

            isRunning = true
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

        private fun showProgressNotification(subtaskName: String, timePerSubtask: Long) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        itemView.context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        itemView.context as Activity, // Передаёшь активити
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1
                    )
                    return
                }
            }
            val notification = NotificationCompat.Builder(itemView.context, channelId)
                .setContentTitle("Выполнение подзадачи")
                .setContentText("$subtaskName: ${formatTime(timePerSubtask / 1000)}")
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

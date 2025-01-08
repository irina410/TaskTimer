import android.app.AlertDialog
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Intent
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Task
import com.example.tasktimer.model.TaskTimerService
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskAdapter(
    private val tasks: MutableList<Task>, // Список задач
    private val onTaskDelete: (Task) -> Unit // Callback для удаления задачи
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        // Создаем ViewHolder для элемента задачи
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        // Привязываем задачу к ViewHolder
        val task = tasks[position]
        holder.bind(task, onTaskDelete)
    }

    override fun getItemCount(): Int = tasks.size // Количество элементов

    // Форматируем время в формат HH:MM:SS
    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // UI элементы
        private val taskNumber: TextView = itemView.findViewById(R.id.taskNumber)
        private val algorithmName: TextView = itemView.findViewById(R.id.taskName)
        private val taskTime: TextView = itemView.findViewById(R.id.taskTime)
        private val startStopButton: FloatingActionButton = itemView.findViewById(R.id.startStopButton)
        private val subtaskLayout: LinearLayout = itemView.findViewById(R.id.subtaskLayout)
        private val subtaskCountdown: TextView = itemView.findViewById(R.id.subtaskCountdown)
        private val currentSubtask: TextView = itemView.findViewById(R.id.currentSubtask)

        // Для управления таймером
        private var isRunning = false
        private var currentSubtaskIndex = 0
        private val notificationManager =
            itemView.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        private val channelId = "TASK_TIMER_CHANNEL"

        init {
            createNotificationChannel() // Создаем канал для уведомлений
        }

        // Привязка задачи к UI
        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            taskNumber.text = task.number.toString() // Номер задачи
            algorithmName.text = task.algorithm.name // Название алгоритма
            taskTime.text = formatTime(task.algorithm.totalTime) // Общее время задачи

            updateButtonIcon(isRunning) // Обновляем иконку кнопки

            // Обработка нажатия кнопки "Старт/Стоп"
            startStopButton.setOnClickListener {
                val serviceIntent = Intent(itemView.context, TaskTimerService::class.java).apply {
                    putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                    putExtra(TaskTimerService.EXTRA_SUBTASK_NAME, task.algorithm.subtasks[0].description)
                    putParcelableArrayListExtra("subtasks", ArrayList(task.algorithm.subtasks))
                    putExtra("totalTime", task.algorithm.totalTime)
                }

                if (isRunning) {
                    // Останавливаем таймер
                    itemView.context.stopService(serviceIntent)
                    stopTaskTimer()
                } else {
                    // Запускаем таймер
                    itemView.context.startService(serviceIntent)
                    startTaskTimer()
                }
            }

            // Удержание элемента вызывает подтверждение удаления
            itemView.setOnLongClickListener {
                showDeleteConfirmationDialog(itemView.context, task, onTaskDelete)
                true
            }
        }

        // Обновление состояния кнопки
        private fun updateButtonIcon(isRunning: Boolean) {
            startStopButton.setImageResource(
                if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }

        // Запуск таймера
        private fun startTaskTimer() {
            isRunning = true
            updateButtonIcon(true)
        }

        // Остановка таймера
        private fun stopTaskTimer() {
            isRunning = false
            subtaskCountdown.text = "Оставшееся время: -"
            currentSubtask.text = "Текущая подзадача: -"
            updateButtonIcon(false)
        }

        // Подтверждение удаления задачи
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

        // Создание канала уведомлений для Android 8+
        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

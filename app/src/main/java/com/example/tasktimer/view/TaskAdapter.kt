package com.example.tasktimer.view

import android.app.AlertDialog
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Intent
import android.content.Context
import android.os.Build
import android.util.Log
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
        Log.d("com.example.tasktimer.view.TaskAdapter", "onCreateViewHolder: Создание нового элемента списка")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        // Привязываем задачу к ViewHolder
        Log.d("com.example.tasktimer.view.TaskAdapter", "onBindViewHolder: Привязка задачи к элементу списка, позиция: $position")
        val task = tasks[position]
        holder.bind(task, onTaskDelete)
    }

    override fun getItemCount(): Int {
        Log.d("com.example.tasktimer.view.TaskAdapter", "getItemCount: Количество элементов в списке: ${tasks.size}")
        return tasks.size // Количество элементов
    }

    private fun formatTime(seconds: Long): String {
        Log.d("com.example.tasktimer.view.TaskAdapter", "formatTime: Форматируем время $seconds секунд в формат HH:MM:SS")
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
        private val subtaskLayout: LinearLayout = itemView.findViewById(R.id.subtaskLayout)
        private val subtaskCountdown: TextView = itemView.findViewById(R.id.subtaskCountdown)
        private val currentSubtask: TextView = itemView.findViewById(R.id.currentSubtask)

        private var isRunning = false
        private var currentSubtaskIndex = 0
        private val notificationManager =
            itemView.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        private val channelId = "TASK_TIMER_CHANNEL"

        init {
            Log.d("com.example.tasktimer.view.TaskAdapter", "TaskViewHolder: Инициализация ViewHolder")
            createNotificationChannel() // Создаем канал для уведомлений
        }

        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            Log.d("com.example.tasktimer.view.TaskAdapter", "TaskViewHolder.bind: Привязываем задачу ${task.number} к UI")

            taskNumber.text = task.number.toString() // Номер задачи
            algorithmName.text = task.algorithm.name // Название алгоритма
            taskTime.text = formatTime(task.algorithm.totalTime) // Общее время задачи

            updateButtonIcon(isRunning) // Обновляем иконку кнопки

            startStopButton.setOnClickListener {
                Log.d("com.example.tasktimer.view.TaskAdapter", "TaskViewHolder.bind: Кнопка 'Старт/Стоп' нажата для задачи ${task.number}")
                val serviceIntent = Intent(itemView.context, TaskTimerService::class.java).apply {
                    putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                    putExtra(TaskTimerService.EXTRA_SUBTASK_NAME, task.algorithm.subtasks[0].description)
                    putParcelableArrayListExtra("subtasks", ArrayList(task.algorithm.subtasks))
                    putExtra("totalTime", task.algorithm.totalTime)
                }

                if (isRunning) {
                    itemView.context.stopService(serviceIntent)
                    stopTaskTimer()
                } else {
                    itemView.context.startService(serviceIntent) // Запускает фоновый сервис TaskTimerService
                    startTaskTimer()
                }
            }

            itemView.setOnLongClickListener {
                Log.d("com.example.tasktimer.view.TaskAdapter", "TaskViewHolder.bind: Долгий клик на задаче ${task.number} для удаления")
                showDeleteConfirmationDialog(itemView.context, task, onTaskDelete)
                true
            }
        }

        private fun updateButtonIcon(isRunning: Boolean) {
            Log.d("com.example.tasktimer.view.TaskAdapter", "updateButtonIcon: Обновление иконки кнопки, состояние: $isRunning")
            startStopButton.setImageResource(
                if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }

        private fun startTaskTimer() {
            Log.d("com.example.tasktimer.view.TaskAdapter", "startTaskTimer: Запуск таймера для задачи")
            isRunning = true
            updateButtonIcon(true)
        }

        private fun stopTaskTimer() {
            Log.d("com.example.tasktimer.view.TaskAdapter", "stopTaskTimer: Остановка таймера для задачи")
            isRunning = false
            subtaskCountdown.text = "Оставшееся время: -"
            currentSubtask.text = "Текущая подзадача: -"
            updateButtonIcon(false)
        }

        private fun showDeleteConfirmationDialog(
            context: Context,
            task: Task,
            onTaskDelete: (Task) -> Unit
        ) {
            Log.d("com.example.tasktimer.view.TaskAdapter", "showDeleteConfirmationDialog: Подтверждение удаления задачи ${task.number}")
            AlertDialog.Builder(context)
                .setTitle("Удалить задачу?")
                .setMessage("Вы точно хотите удалить задачу №${task.number} (${task.algorithm.name})?")
                .setPositiveButton("Удалить") { _, _ -> onTaskDelete(task) }
                .setNegativeButton("Отмена", null)
                .show()
        }

        private fun createNotificationChannel() {
            Log.d("com.example.tasktimer.view.TaskAdapter", "createNotificationChannel: Создание канала уведомлений для Android 8+")
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

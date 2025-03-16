package com.example.tasktimer.view

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Task
import com.example.tasktimer.model.TaskTimerService
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
        holder.bind(tasks[position], onTaskDelete)
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Объявляем все View элементы
        private val taskNumber: TextView = itemView.findViewById(R.id.taskNumber)
        private val algorithmName: TextView = itemView.findViewById(R.id.taskName)
        private val taskTime: TextView = itemView.findViewById(R.id.taskTime)
        private val startStopButton: FloatingActionButton = itemView.findViewById(R.id.startStopButton)
        private val subtaskLayout: ViewGroup = itemView.findViewById(R.id.subtaskLayout)
        private val subtaskCountdown: TextView = itemView.findViewById(R.id.subtaskCountdown)
        private val currentSubtask: TextView = itemView.findViewById(R.id.currentSubtask)
        private val nextSubtask: TextView = itemView.findViewById(R.id.nextSubtask)
        private var isRunning = false
        private val context: Context = itemView.context // Получаем контекст

        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            taskNumber.text = task.number.toString()
            algorithmName.text = task.algorithm.name
            taskTime.text = formatTime(task.algorithm.totalTime)

            // Получаем подзадачи из задачи
            val subtasks = task.algorithm.subtasks

            val prefs = context.getSharedPreferences("TaskProgress", Context.MODE_PRIVATE)
            val currentIndex = prefs.getInt("task_${task.number}_current", -1)

            if (currentIndex != -1 && currentIndex < subtasks.size) {
                subtaskLayout.visibility = View.VISIBLE
                val remaining = prefs.getLong("task_${task.number}_remaining", 0) / 1000
                subtaskCountdown.text = "Осталось: ${formatTime(remaining)}"
                currentSubtask.text = "Текущая: ${subtasks[currentIndex].description}"

                // Получаем следующую подзадачу
                val nextSubtaskDesc = prefs.getString(
                    "task_${task.number}_next_desc",
                    if (currentIndex + 1 < subtasks.size) subtasks[currentIndex + 1].description
                    else "Завершено"
                )
                nextSubtask.text = "Следующая: $nextSubtaskDesc"
            } else {
                subtaskLayout.visibility = View.GONE
            }

            startStopButton.setOnClickListener {
                isRunning = !isRunning
                updateButtonIcon()
                if (isRunning) startTask(task) else stopTask(task)
            }

            itemView.setOnLongClickListener {
                showDeleteConfirmationDialog(context, task, onTaskDelete)
                true
            }
        }


        private fun startTask(task: Task) {
            Intent(context, TaskTimerService::class.java).apply {
                putExtra(TaskTimerService.EXTRA_TASK_NUMBER, task.number)
                putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                putParcelableArrayListExtra("subtasks", ArrayList(task.algorithm.subtasks))
                context.startService(this)
            }
        }

        private fun stopTask(task: Task) {
            Intent(context, TaskTimerService::class.java).apply {
                action = TaskTimerService.ACTION_STOP_TASK
                putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                context.startService(this)
            }

            context.getSharedPreferences("TaskProgress", Context.MODE_PRIVATE).edit().apply {
                remove("task_${task.number}_current")
                remove("task_${task.number}_remaining")
                remove("task_${task.number}_next_desc")
                apply()
            }
        }

        private fun updateButtonIcon() {
            startStopButton.setImageResource(
                if (isRunning) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        private fun formatTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return "%02d:%02d:%02d".format(hours, minutes, secs)
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


    }
}

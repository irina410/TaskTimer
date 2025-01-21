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
    private val tasks: MutableList<Task>, // Список задач
    private val onTaskDelete: (Task) -> Unit // Callback для удаления задачи
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var currentTaskIndex: Int = -1 // Индекс задачи с текущей подзадачей
    private var currentSubtaskIndex: Int = -1 // Индекс текущей подзадачи

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task, onTaskDelete)

        // Обновляем текущую подзадачу, если она относится к этой задаче
        if (position == currentTaskIndex) {
            holder.updateCurrentSubtask(currentSubtaskIndex)
        } else {
            holder.clearCurrentSubtask()
        }
    }

    override fun getItemCount(): Int = tasks.size

    /**
     * Обновляет текущую задачу и подзадачу.
     */
    fun updateCurrentSubtask(taskIndex: Int, subtaskIndex: Int) {
        val previousTaskIndex = currentTaskIndex
        currentTaskIndex = taskIndex
        currentSubtaskIndex = subtaskIndex

        if (previousTaskIndex != -1) notifyItemChanged(previousTaskIndex)
        if (currentTaskIndex != -1) notifyItemChanged(currentTaskIndex)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val taskNumber: TextView = itemView.findViewById(R.id.taskNumber)
        private val algorithmName: TextView = itemView.findViewById(R.id.taskName)
        private val taskTime: TextView = itemView.findViewById(R.id.taskTime)
        private val startStopButton: FloatingActionButton = itemView.findViewById(R.id.startStopButton)
        private val subtaskCountdown: TextView = itemView.findViewById(R.id.subtaskCountdown)
        private val currentSubtask: TextView = itemView.findViewById(R.id.currentSubtask)

        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            taskNumber.text = task.number.toString()
            algorithmName.text = task.algorithm.name
            taskTime.text = formatTime(task.algorithm.totalTime)

            startStopButton.setOnClickListener {
                val serviceIntent = Intent(itemView.context, TaskTimerService::class.java).apply {
                    putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                    putParcelableArrayListExtra("subtasks", ArrayList(task.algorithm.subtasks))
                }

                itemView.context.startService(serviceIntent)
            }

            itemView.setOnLongClickListener {
                showDeleteConfirmationDialog(itemView.context, task, onTaskDelete)
                true
            }
        }

        /**
         * Обновляет отображение текущей подзадачи.
         */
        fun updateCurrentSubtask(index: Int) {
            val task = tasks[adapterPosition]
            val subtask = task.algorithm.subtasks.getOrNull(index)

            if (subtask != null) {
                subtaskCountdown.text = "Оставшееся время: ${formatTime(subtask.duration)}"
                currentSubtask.text = "Текущая подзадача: ${subtask.description}"
            } else {
                clearCurrentSubtask()
            }
        }

        /**
         * Очищает отображение текущей подзадачи.
         */
        fun clearCurrentSubtask() {
            subtaskCountdown.text = "Оставшееся время: -"
            currentSubtask.text = "Текущая подзадача: -"
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

        private fun formatTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
}

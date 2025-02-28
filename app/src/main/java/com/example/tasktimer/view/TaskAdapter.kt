package com.example.tasktimer.view

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

        private val taskNumber: TextView = itemView.findViewById(R.id.taskNumber)
        private val algorithmName: TextView = itemView.findViewById(R.id.taskName)
        private val taskTime: TextView = itemView.findViewById(R.id.taskTime)
        private val startStopButton: FloatingActionButton = itemView.findViewById(R.id.startStopButton)
        private var isRunning = false
        private val subtaskLayout: LinearLayout = itemView.findViewById(R.id.subtaskLayout)

        fun bind(task: Task, onTaskDelete: (Task) -> Unit) {
            taskNumber.text = task.number.toString()
            algorithmName.text = task.algorithm.name
            taskTime.text = formatTime(task.algorithm.totalTime)

            startStopButton.setOnClickListener {
                isRunning = !isRunning
                updateButtonIcon()

                if (isRunning) {
                    startTask(task)
                } else {
                    stopTask(task)
                }
            }

            itemView.setOnLongClickListener {
                showDeleteConfirmationDialog(itemView.context, task, onTaskDelete)
                true
            }
        }

        private fun startTask(task: Task) {
            val serviceIntent = Intent(itemView.context, TaskTimerService::class.java).apply {
                putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
                putParcelableArrayListExtra("subtasks", ArrayList(task.algorithm.subtasks))
            }

            // Сохраняем данные в SharedPreferences
            val sharedPrefs = itemView.context.getSharedPreferences("AlarmPrefs", Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putInt("TASK_NUMBER",task.number)
                putString("TOTAL_TIME", formatTime(task.algorithm.totalTime))
                apply()
            }
            itemView.context.startService(serviceIntent)
        }

        private fun stopTask(task: Task) {
            val stopIntent = Intent(itemView.context, TaskTimerService::class.java).apply {
                action = TaskTimerService.ACTION_STOP_TASK
                putExtra(TaskTimerService.EXTRA_TASK_NAME, task.algorithm.name)
            }
            itemView.context.startService(stopIntent)
        }

        private fun updateButtonIcon() {
            startStopButton.setImageResource(
                if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
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
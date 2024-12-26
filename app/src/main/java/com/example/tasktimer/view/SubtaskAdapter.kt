package com.example.tasktimer.view

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Subtask

class SubtaskAdapter(
    private val subtasks: MutableList<Subtask>,
    private val onSubtaskUpdated: () -> Unit, // Колбэк для обновления общего времени
    private val onSubtaskDeleted: (Int) -> Unit // Колбэк для удаления подзадачи
) : RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtask, parent, false)
        return SubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        val subtask = subtasks[position]
        holder.bind(subtask, onSubtaskUpdated, onSubtaskDeleted)
    }

    override fun getItemCount(): Int = subtasks.size

    class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameEditText: EditText = itemView.findViewById(R.id.subtaskName)
        private val durationEditText: EditText = itemView.findViewById(R.id.subtaskDuration)
        private val deleteButton: View = itemView.findViewById(R.id.deleteButton)

        private fun parseTimeToSeconds(time: String): Long {
            val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
            return when (parts.size) {
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]).toLong()
                2 -> (parts[0] * 60 + parts[1]).toLong()
                1 -> parts[0].toLong()
                else -> 0
            }
        }

        private fun formatSecondsToTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }

        fun bind(subtask: Subtask, onSubtaskUpdated: () -> Unit, onSubtaskDeleted: (Int) -> Unit) {
            nameEditText.setText(subtask.description)
            durationEditText.setText(if (subtask.duration == 0L) "" else formatSecondsToTime(subtask.duration))

            nameEditText.setOnFocusChangeListener { _, _ ->
                subtask.description = nameEditText.text.toString()
                onSubtaskUpdated()
            }

            durationEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val durationText = durationEditText.text.toString()
                    subtask.duration = if (durationText.isNotBlank()) parseTimeToSeconds(durationText) else 0L
                    durationEditText.setText(formatSecondsToTime(subtask.duration))
                    onSubtaskUpdated()
                }
            }

            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSubtaskDeleted(position)
                }
            }
        }
    }
}

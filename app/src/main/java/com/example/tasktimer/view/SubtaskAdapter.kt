package com.example.tasktimer.view

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Subtask

class SubtaskAdapter(
    private val subtasks: MutableList<Subtask>,
    private val onSubtaskChanged: () -> Unit // Колбэк для обновления общего времени
) : RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subtask, parent, false)
        return SubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        val subtask = subtasks[position]
        holder.bind(subtask, onSubtaskChanged)
    }

    override fun getItemCount(): Int = subtasks.size

    class SubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameEditText: EditText = itemView.findViewById(R.id.subtaskName)
        private val durationEditText: EditText = itemView.findViewById(R.id.subtaskDuration)

        // Преобразование строки чч:мм:сс в секунды
        private fun parseTimeToSeconds(time: String): Long {
            val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
            return when (parts.size) {
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]).toLong()
                2 -> (parts[0] * 60 + parts[1]).toLong()
                1 -> parts[0].toLong()
                else -> 0
            }
        }

        // Преобразование секунд в чч:мм:сс
        private fun formatSecondsToTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }

        // Установка маски для времени
        private fun setupTimeMask(editText: EditText) {
            editText.addTextChangedListener(object : TextWatcher {
                private var isEditing = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isEditing) return
                    isEditing = true

                    val input = s.toString().replace(":", "").padStart(0, '0') // Убираем двоеточия
                    if (input.isNotBlank() && input.length <= 6) {
                        val hours = input.padStart(6, '0').substring(0, 2)
                        val minutes = input.padStart(6, '0').substring(2, 4)
                        val seconds = input.padStart(6, '0').substring(4, 6)
                        editText.setText("$hours:$minutes:$seconds")
                        editText.setSelection(editText.text.length)
                    }

                    isEditing = false
                }
            })
        }


        fun bind(subtask: Subtask, onSubtaskChanged: () -> Unit) {
            nameEditText.setText(subtask.description)

            // Оставляем поле пустым, если время = 0
            val initialTime = if (subtask.duration == 0L) "" else formatSecondsToTime(subtask.duration)
            durationEditText.setText(initialTime)

            //setupTimeMask(durationEditText) // Подключаем маску для ввода

            nameEditText.setOnFocusChangeListener { _, _ ->
                subtask.description = nameEditText.text.toString()
                onSubtaskChanged()
            }

            durationEditText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val durationText = durationEditText.text.toString()
                    if (durationText.isNotBlank()) { // Только если поле не пустое
                        subtask.duration = parseTimeToSeconds(durationText)
                        durationEditText.setText(formatSecondsToTime(subtask.duration)) // Показываем формат чч:мм:сс
                    } else {
                        subtask.duration = 0L // Сбрасываем время, если поле пустое
                    }
                    onSubtaskChanged()
                }
            }
        }

    }


}

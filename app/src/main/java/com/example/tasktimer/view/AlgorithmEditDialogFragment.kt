package com.example.tasktimer.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm
import com.example.tasktimer.model.Subtask

class AlgorithmEditDialogFragment(
    private val algorithm: Algorithm?, // null, если создаём новый
    private val onSave: (Algorithm) -> Unit // Лямбда для сохранения
) : DialogFragment() {

    private lateinit var nameEditText: EditText
    private lateinit var totalTimeTextView: TextView
    private lateinit var subtasksRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView

    private val subtasks = mutableListOf<Subtask>() // Локальный список подзадач

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_algorithm_edit_dialog, container, false)

        // Инициализация UI
        nameEditText = view.findViewById(R.id.editAlgorithmName)
        totalTimeTextView = view.findViewById(R.id.totalTime)
        subtasksRecyclerView = view.findViewById(R.id.subtasksRecyclerView)
        emptyTextView = view.findViewById(R.id.emptySubtasksText)

        // Если алгоритм передан, заполняем поля
        algorithm?.let {
            nameEditText.setText(it.name)
            subtasks.addAll(it.subtasks)
            updateSubtasks()
        }

        // Настраиваем RecyclerView
        subtasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        subtasksRecyclerView.adapter = SubtaskAdapter(subtasks) { updateTotalTime() }

        // Кнопки
        view.findViewById<View>(R.id.addSubtaskFab).setOnClickListener {
            subtasks.add(Subtask("", 0))
            updateSubtasks()
        }

        view.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dismiss() // Закрываем диалог
        }

        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val nameText = nameEditText.text.toString().trim() // Убираем пробелы по краям
            if (nameText.isNotEmpty()) { // Проверяем, что текст не пустой
                val newAlgorithm = Algorithm(
                    name = nameText, // Используем введённое название
                    subtasks = subtasks.toList() // Копируем подзадачи
                )
                onSave(newAlgorithm) // Вызываем коллбэк для сохранения
                dismiss() // Закрываем диалог
            } else {
                // Если поле пустое, показываем сообщение пользователю
                Toast.makeText(requireContext(), "Введите название алгоритма", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun updateSubtasks() {
        if (subtasks.isEmpty()) {
            emptyTextView.visibility = View.VISIBLE
            subtasksRecyclerView.visibility = View.GONE
        } else {
            emptyTextView.visibility = View.GONE
            subtasksRecyclerView.visibility = View.VISIBLE
            subtasksRecyclerView.adapter?.notifyDataSetChanged()
        }
        updateTotalTime()
    }

    private fun updateTotalTime() {
        val totalSeconds = subtasks.sumOf { it.duration }
        totalTimeTextView.text = formatTime(totalSeconds)
    }

    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

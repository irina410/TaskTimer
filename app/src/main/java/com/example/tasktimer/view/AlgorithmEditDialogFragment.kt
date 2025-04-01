package com.example.tasktimer.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
    private val algorithm: Algorithm?,
    private val onSave: (Algorithm) -> Unit,
    private val onDelete: (Algorithm) -> Unit
) : DialogFragment() {

    private lateinit var nameEditText: EditText
    private lateinit var totalTimeTextView: TextView
    private lateinit var subtasksRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView

    private val subtasks = mutableListOf<Subtask>()


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_algorithm_edit_dialog, container, false)

        // Инициализация элементов интерфейса
        nameEditText = view.findViewById(R.id.editAlgorithmName)
        totalTimeTextView = view.findViewById(R.id.totalTime)
        subtasksRecyclerView = view.findViewById(R.id.subtasksRecyclerView)
        emptyTextView = view.findViewById(R.id.emptySubtasksText)

        // Если передан существующий алгоритм, заполняем его данные в поля
        algorithm?.let {
            nameEditText.setText(it.name)
            subtasks.addAll(it.subtasks)
        }

        // Настройка RecyclerView для отображения списка подзадач
        subtasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        subtasksRecyclerView.adapter = SubtaskAdapter(
            subtasks,
            ::onSubtaskUpdated
        ) { position ->
            subtasks.removeAt(position)
            updateSubtasks()
        }

        updateSubtasks()

        // Обработчик для добавления новой подзадачи
        view.findViewById<View>(R.id.addSubtaskFab).setOnClickListener {
            subtasks.add(Subtask("", 0))
            updateSubtasks()
        }

        // Обработчик для закрытия диалога
        view.findViewById<View>(R.id.closeButton).setOnClickListener {
            dismiss()
        }

        // Обработчик для сохранения алгоритма
        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val nameText = nameEditText.text.toString().trim()
            if (nameText.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название алгоритма", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Создаем обновленный алгоритм на основе данных из интерфейса
            val updatedAlgorithm = Algorithm(
                name = nameText,
                subtasks = subtasks.toList()
            ).apply { recalculateTotalTime() }

            // Если алгоритм редактируется, удаляем старую версию
            algorithm?.let { onDelete(it) }
            // Отправляем броадкаст для обновления списка задач
            val intent = Intent("ALGORITHM_UPDATED").apply {
                putExtra("algorithm_id", updatedAlgorithm.name) // Передаём ID, если нужно
            }
            requireContext().sendBroadcast(intent)
            onSave(updatedAlgorithm)

            dismiss()
        }

        // Обработчик для удаления алгоритма
        view.findViewById<View>(R.id.deleteButton).setOnClickListener {
            algorithm?.let {
                onDelete(it)
            }
            dismiss()
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        // Скрываем клавиатуру при сворачивании диалога
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val view = requireActivity().currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
    }

    /**
     * Обновление отображения списка подзадач.
     * Если список пуст, показываем сообщение "Нет подзадач".
     */
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

    /**
     * Вызывается при обновлении одной из подзадач.
     * Пересчитывает общее время выполнения алгоритма.
     */
    private fun onSubtaskUpdated() {
        updateTotalTime()
    }

    /**
     * Обновление общего времени выполнения алгоритма.
     * Вычисляет сумму времени всех подзадач и отображает в интерфейсе.
     */
    private fun updateTotalTime() {
        val totalSeconds = subtasks.sumOf { it.duration }
        totalTimeTextView.text = formatTime(totalSeconds)
    }

    /**
     * Форматирование времени в виде HH:MM:SS.
     *
     * @param totalSeconds Общее количество секунд.
     * @return Отформатированная строка.
     */
    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

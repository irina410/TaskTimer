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

/**
 * Диалог для создания или редактирования алгоритма.
 * @param algorithm Если null, то создаётся новый алгоритм. Если передан, редактируется существующий.
 * @param onSave Лямбда-функция, которая вызывается при сохранении алгоритма.
 */
class AlgorithmEditDialogFragment(
    private val algorithm: Algorithm?, // null, если создаём новый алгоритм
    private val onSave: (Algorithm) -> Unit // Колбэк для сохранения
) : DialogFragment() {

    // Поля для интерфейса
    private lateinit var nameEditText: EditText // Поле для ввода названия алгоритма
    private lateinit var totalTimeTextView: TextView // Поле для отображения общего времени
    private lateinit var subtasksRecyclerView: RecyclerView // RecyclerView для списка подзадач
    private lateinit var emptyTextView: TextView // Текст "Список пуст" для пустых подзадач

    private val subtasks = mutableListOf<Subtask>() // Локальный список подзадач

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Инфлейтим макет
        val view = inflater.inflate(R.layout.fragment_algorithm_edit_dialog, container, false)

        // Инициализация UI-элементов
        nameEditText = view.findViewById(R.id.editAlgorithmName) // Поле ввода названия
        totalTimeTextView = view.findViewById(R.id.totalTime) // Текст общего времени
        subtasksRecyclerView = view.findViewById(R.id.subtasksRecyclerView) // RecyclerView подзадач
        emptyTextView = view.findViewById(R.id.emptySubtasksText) // Текст "Список пуст"

        // Если алгоритм передан (редактируем), заполняем поля значениями
        algorithm?.let {
            nameEditText.setText(it.name) // Устанавливаем название
            subtasks.addAll(it.subtasks) // Копируем подзадачи в локальный список
            updateSubtasks() // Обновляем UI для подзадач
        }

        // Настраиваем RecyclerView для подзадач
        subtasksRecyclerView.layoutManager = LinearLayoutManager(requireContext()) // Вертикальный список
        subtasksRecyclerView.adapter = SubtaskAdapter(subtasks) { updateTotalTime() } // Адаптер с обновлением времени

        // Кнопка добавления новой подзадачи
        view.findViewById<View>(R.id.addSubtaskFab).setOnClickListener {
            subtasks.add(Subtask("", 0)) // Добавляем новую подзадачу
            updateSubtasks() // Обновляем UI
        }

        // Кнопка "Отмена" для закрытия диалога
        view.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dismiss() // Закрываем диалог
        }

        // Кнопка "Сохранить" для сохранения алгоритма
        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val nameText = nameEditText.text.toString().trim() // Получаем название, удаляя лишние пробелы
            if (nameText.isNotEmpty()) { // Если название введено
                val newAlgorithm = Algorithm(
                    name = nameText, // Устанавливаем введённое название
                    subtasks = subtasks.toList() // Копируем список подзадач
                )
                onSave(newAlgorithm) // Вызываем колбэк сохранения
                dismiss() // Закрываем диалог
            } else {
                // Если название пустое, показываем сообщение
                Toast.makeText(requireContext(), "Введите название алгоритма", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    /**
     * Обновляет список подзадач на экране.
     * Если список пуст, показывает текст "Список пуст". Если нет, показывает список.
     */
    private fun updateSubtasks() {
        if (subtasks.isEmpty()) {
            // Если нет подзадач, показываем текст "Список пуст" и скрываем RecyclerView
            emptyTextView.visibility = View.VISIBLE
            subtasksRecyclerView.visibility = View.GONE
        } else {
            // Если подзадачи есть, скрываем текст "Список пуст" и показываем RecyclerView
            emptyTextView.visibility = View.GONE
            subtasksRecyclerView.visibility = View.VISIBLE
            subtasksRecyclerView.adapter?.notifyDataSetChanged() // Уведомляем адаптер об изменениях
        }
        updateTotalTime() // Пересчитываем общее время
    }

    /**
     * Пересчитывает и обновляет общее время алгоритма.
     */
    private fun updateTotalTime() {
        val totalSeconds = subtasks.sumOf { it.duration } // Суммируем время всех подзадач
        totalTimeTextView.text = formatTime(totalSeconds) // Форматируем и отображаем
    }

    /**
     * Форматирует время из секунд в формат "чч:мм:сс".
     * @param totalSeconds Общее количество секунд.
     * @return Отформатированное время.
     */
    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600 // Часы
        val minutes = (totalSeconds % 3600) / 60 // Минуты
        val seconds = totalSeconds % 60 // Секунды
        return String.format("%02d:%02d:%02d", hours, minutes, seconds) // Формат "чч:мм:сс"
    }
}

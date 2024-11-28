package com.example.tasktimer.view

import AlgorithmAdapter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Task
import com.example.tasktimer.model.Algorithm

class TaskCreateFragment(
    private val onTaskCreated: (Task) -> Unit,
    private val taskList: List<Task>, // Список существующих задач
    private val algorithms: List<Algorithm> // Список доступных алгоритмов
) : Fragment() {

    private lateinit var taskNumberEditText: EditText
    private lateinit var algorithmRecyclerView: RecyclerView
    private var selectedAlgorithm: Algorithm? = null // Выбранный алгоритм

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_create, container, false)

        taskNumberEditText = view.findViewById(R.id.taskNumber)
        algorithmRecyclerView = view.findViewById(R.id.algorithmRecyclerView)

        // Настройка кнопки закрытия
        view.findViewById<View>(R.id.closeButton).setOnClickListener {
            parentFragmentManager.popBackStack() // Закрытие фрагмента
        }

        // Устанавливаем адаптер для RecyclerView
        algorithmRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val algorithmAdapter = AlgorithmAdapter(algorithms) { algorithm ->
            selectedAlgorithm = algorithm // Сохраняем выбранный алгоритм
        }
        algorithmRecyclerView.adapter = algorithmAdapter

        // Обработка кнопки сохранения
        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val taskNumber = taskNumberEditText.text.toString().trim()

            if (TextUtils.isEmpty(taskNumber)) {
                taskNumberEditText.error = "Введите номер задачи"
            } else if (!isTaskNumberUnique(taskNumber.toInt())) {
                taskNumberEditText.error = "Этот номер уже существует"
            } else if (selectedAlgorithm == null) {
                Toast.makeText(requireContext(), "Выберите алгоритм", Toast.LENGTH_SHORT).show()
            } else {
                // Создание новой задачи
                val newTask = Task(
                    number = taskNumber.toInt(),
                    algorithm = selectedAlgorithm!!
                )
                onTaskCreated(newTask) // Возвращаем задачу
                parentFragmentManager.popBackStack() // Закрываем фрагмент
            }
        }

        return view
    }

    private fun isTaskNumberUnique(taskNumber: Int): Boolean {
        return taskList.none { it.number == taskNumber }
    }
}

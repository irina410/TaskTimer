package com.example.tasktimer.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Task
import com.example.tasktimer.repository.AlgorithmRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskFragment : Fragment() {
    private val tasks = mutableListOf<Task>() // Список задач
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task, container, false)
        loadTasks()

        // Инициализация RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter(tasks) { task -> deleteTask(task) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = taskAdapter

        // Обработчик кнопки FAB
        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Получаем доступные алгоритмы
            val availableAlgorithms = AlgorithmRepository(requireContext()).loadAlgorithms()

            val taskCreateFragment = TaskCreateFragment(
                onTaskCreated = { newTask ->
                    tasks.add(newTask) // Добавляем новую задачу в список
                    tasks.sortBy { it.number } // Сортировка по номеру задачи
                    taskAdapter.notifyDataSetChanged() // Обновляем адаптер
                    saveTasks()
                    Toast.makeText(requireContext(), "Задача создана!", Toast.LENGTH_SHORT).show()
                },
                taskList = tasks,
                algorithms = availableAlgorithms
            )

            // Показываем TaskCreateFragment как диалог
            taskCreateFragment.show(parentFragmentManager, "taskCreateDialog")
        }

        return view
    }

    private fun deleteTask(task: Task) {
        tasks.remove(task)
        taskAdapter.notifyDataSetChanged()
        saveTasks()
        Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
    }

    private fun saveTasks() {
        val sharedPreferences = requireContext().getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Преобразуем список задач в JSON-строку
        val jsonString = Gson().toJson(tasks)
        editor.putString("tasks", jsonString)
        editor.apply()
    }

    private fun loadTasks() {
        val sharedPreferences = requireContext().getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("tasks", null)

        if (!jsonString.isNullOrEmpty()) {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            tasks.clear()
            tasks.addAll(Gson().fromJson(jsonString, type))
            tasks.sortBy { it.number } // Сортировка по номеру задачи

        }
    }

}

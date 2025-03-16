package com.example.tasktimer.view

import android.content.Context
import android.content.SharedPreferences
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
    private val tasks = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var prefs: SharedPreferences // Добавляем переменную

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализируем prefs
        prefs = requireContext().getSharedPreferences("TaskProgress", Context.MODE_PRIVATE)

        val view = inflater.inflate(R.layout.fragment_task, container, false)
        loadTasks()

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter(tasks) { task -> deleteTask(task) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = taskAdapter

        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val availableAlgorithms = AlgorithmRepository(requireContext()).loadAlgorithms()
            val taskCreateFragment = TaskCreateFragment(
                onTaskCreated = { newTask ->
                    tasks.add(newTask)
                    tasks.sortBy { it.number }
                    taskAdapter.notifyDataSetChanged()
                    saveTasks()
                    Toast.makeText(requireContext(), "Задача создана!", Toast.LENGTH_SHORT).show()
                },
                taskList = tasks,
                algorithms = availableAlgorithms
            )
            taskCreateFragment.show(parentFragmentManager, "taskCreateDialog")
        }

        return view
    }

    override fun onResume() {
        super.onResume() // Добавляем вызов super
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onPause() {
        super.onPause() // Добавляем вызов super
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun deleteTask(task: Task) {
        tasks.remove(task)
        taskAdapter.notifyDataSetChanged()
        saveTasks()
        Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
    }

    private fun saveTasks() {
        val sharedPreferences = requireContext().getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("tasks", Gson().toJson(tasks))
            apply()
        }
    }

    private fun loadTasks() {
        val sharedPreferences = requireContext().getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("tasks", null)
        if (!jsonString.isNullOrEmpty()) {
            tasks.clear()
            tasks.addAll(Gson().fromJson(jsonString, object : TypeToken<MutableList<Task>>() {}.type))
            tasks.sortBy { it.number }
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("task_") == true) {
            taskAdapter.notifyDataSetChanged()
        }
    }
}
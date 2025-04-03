package com.example.tasktimer.view

import TaskAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
    private lateinit var prefs: SharedPreferences
    private val algorithmUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "ALGORITHM_UPDATED") {
                val algorithmName = intent.getStringExtra("algorithm_id")
                updateTasksWithNewAlgorithm(algorithmName)
            }
        }
    }
    override fun onStart() {
        super.onStart()

        val filter = IntentFilter("ALGORITHM_UPDATED")
        ContextCompat.registerReceiver(
            requireContext(),
            algorithmUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

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
        super.onResume()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onPause() {
        super.onPause()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
    override fun onStop() {
        super.onStop()

        requireContext().unregisterReceiver(algorithmUpdateReceiver)

        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
    private fun updateTasksWithNewAlgorithm(algorithmName: String?) {
        algorithmName ?: return

        val repository = AlgorithmRepository(requireContext())
        val updatedAlgorithm = repository.loadAlgorithms().find { it.name == algorithmName }

        updatedAlgorithm?.let { algo ->

            tasks.forEach { task ->
                if (task.algorithm.name == algorithmName) {
                    task.algorithm = algo.copy()
                }
            }

            saveTasks()
            taskAdapter.notifyDataSetChanged()
        }
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
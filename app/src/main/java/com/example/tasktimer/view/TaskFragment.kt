package com.example.tasktimer.view

import TaskAdapter
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
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskFragment : Fragment() {
    private val tasks = mutableListOf<Task>() // Список задач
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task, container, false)

        // Инициализация RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter(tasks) { task -> deleteTask(task) }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = taskAdapter

        // Обработчик кнопки FAB
        view.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Логика для добавления новой задачи
        }

        return view
    }

    private fun deleteTask(task: Task) {
        tasks.remove(task)
        taskAdapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
    }
}

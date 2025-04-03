package com.example.tasktimer.view

import AlgorithmAdapter
import AlgorithmExpandableAdapter
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Task
import com.example.tasktimer.model.Algorithm

class TaskCreateFragment(
    private val onTaskCreated: (Task) -> Unit,
    private val taskList: List<Task>,
    private val algorithms: List<Algorithm>
) : DialogFragment() {

    private lateinit var taskNumberEditText: EditText
    private lateinit var algorithmRecyclerView: RecyclerView
    private lateinit var selectedAlgorithmRecyclerView: RecyclerView
    private lateinit var switchHighPriority: Switch

    private lateinit var selectedAlgorithmAdapter: AlgorithmExpandableAdapter
    private var selectedAlgorithm: Algorithm? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_task_create, container, false)

        taskNumberEditText = view.findViewById(R.id.taskNumber)
        algorithmRecyclerView = view.findViewById(R.id.algorithmRecyclerView)
        selectedAlgorithmRecyclerView = view.findViewById(R.id.selectedAlgorithmResV)

        val algorithmAdapter = AlgorithmAdapter(algorithms) { algorithm ->
            onAlgorithmSelected(algorithm)
        }
        algorithmRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        algorithmRecyclerView.adapter = algorithmAdapter

        selectedAlgorithmRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        selectedAlgorithmAdapter = AlgorithmExpandableAdapter(emptyList()) { }
        selectedAlgorithmRecyclerView.adapter = selectedAlgorithmAdapter

        view.findViewById<View>(R.id.closeButton).setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val taskNumber = taskNumberEditText.text.toString().trim()
            if (TextUtils.isEmpty(taskNumber)) {
                taskNumberEditText.error = "Введите номер задачи"
            } else if (selectedAlgorithm == null) {
                Toast.makeText(requireContext(), "Выберите алгоритм", Toast.LENGTH_SHORT).show()
            } else if (!isTaskNumberUnique(taskNumber.toInt())) {
                taskNumberEditText.error = "Этот номер уже существует"
            } else {

                onTaskCreated(Task(number = taskNumber.toInt(), algorithm = selectedAlgorithm!!))
                dismiss()
            }
        }

        return view
    }

    private fun onAlgorithmSelected(algorithm: Algorithm) {
        selectedAlgorithm = algorithm

        selectedAlgorithmRecyclerView.visibility = View.VISIBLE
        val updatedList = listOf(algorithm)
        selectedAlgorithmAdapter.updateData(updatedList)
    }

    private fun isTaskNumberUnique(taskNumber: Int): Boolean {
        return taskList.none { it.number == taskNumber }
    }
}

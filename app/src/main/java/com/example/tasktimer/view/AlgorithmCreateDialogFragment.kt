package com.example.tasktimer.view

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

class AlgorithmCreateDialogFragment(
    private val algorithm: Algorithm?,
    private val onSave: (Algorithm) -> Unit
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
        val view = inflater.inflate(R.layout.fragment_algorithm_create_dialog, container, false)

        nameEditText = view.findViewById(R.id.editAlgorithmName)
        totalTimeTextView = view.findViewById(R.id.totalTime)
        subtasksRecyclerView = view.findViewById(R.id.subtasksRecyclerView)
        emptyTextView = view.findViewById(R.id.emptySubtasksText)

        algorithm?.let {
            nameEditText.setText(it.name)
            subtasks.addAll(it.subtasks)
        }

        subtasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        subtasksRecyclerView.adapter = SubtaskAdapter(subtasks, ::updateTotalTime) { position ->
            subtasks.removeAt(position)
            updateSubtasks()
        }

        updateSubtasks()

        view.findViewById<View>(R.id.addSubtaskFab).setOnClickListener {
            subtasks.add(Subtask("", 0))
            updateSubtasks()
        }

        view.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dismiss()
        }

        view.findViewById<View>(R.id.saveButton).setOnClickListener {
            val nameText = nameEditText.text.toString().trim()
            if (nameText.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название алгоритма", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val newAlgorithm = Algorithm(
                name = nameText,
                subtasks = subtasks.toList()
            )

            onSave(newAlgorithm)
            dismiss()
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val view = requireActivity().currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        }
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

package com.example.tasktimer.view

import AlgorithmAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.repository.AlgorithmRepository
import com.example.tasktimer.viewmodel.AlgorithmViewModel

class AlgorithmFragment : Fragment() {

    private lateinit var viewModel: AlgorithmViewModel
    private lateinit var adapter: AlgorithmAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_algorithm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = AlgorithmRepository(requireContext())
        viewModel = AlgorithmViewModel(repository)

        viewModel.loadAlgorithms()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = AlgorithmAdapter(viewModel.algorithms) { selectedAlgorithm ->
            AlgorithmEditDialogFragment(
                algorithm = selectedAlgorithm,
                onSave = { updatedAlgorithm ->
                    updatedAlgorithm.recalculateTotalTime()
                    viewModel.addAlgorithm(updatedAlgorithm)
                    adapter.submitList(viewModel.algorithms)
                },
                onDelete = { deletedAlgorithm ->
                    viewModel.removeAlgorithm(deletedAlgorithm)
                    adapter.submitList(viewModel.algorithms)
                }
            ).show(parentFragmentManager, "editAlgorithm")
        }


        recyclerView.adapter = adapter

        adapter.submitList(viewModel.algorithms)

        view.findViewById<View>(R.id.fab).setOnClickListener {
            AlgorithmCreateDialogFragment(null) { newAlgorithm ->
                newAlgorithm.recalculateTotalTime()
                viewModel.addAlgorithm(newAlgorithm)
                adapter.submitList(viewModel.algorithms)
            }.show(parentFragmentManager, "addAlgorithm")
        }
    }
}

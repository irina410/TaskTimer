package com.example.tasktimer.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm
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

        // Создаём экземпляр ViewModel с репозиторием
        val repository = AlgorithmRepository(requireContext())
        viewModel = AlgorithmViewModel(repository)

        // Загружаем данные
        viewModel.loadAlgorithms()

        // Настраиваем RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = AlgorithmAdapter()
        recyclerView.adapter = adapter

        // Передаём данные из ViewModel в адаптер
        adapter.submitList(viewModel.algorithms)

        // Пример добавления нового алгоритма (можно использовать FAB или другой способ)
        val newAlgorithm = Algorithm("Пример алгоритма", listOf())
        viewModel.addAlgorithm(newAlgorithm)

        // Обновляем список в адаптере
        adapter.submitList(viewModel.algorithms)
    }
}

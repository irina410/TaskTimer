package com.example.tasktimer.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tasktimer.model.Algorithm
import com.example.tasktimer.repository.AlgorithmRepository

class AlgorithmViewModel(private val repository: AlgorithmRepository) : ViewModel() {

    var algorithms: List<Algorithm> = emptyList()
        private set

    // Загружаем данные при старте
    fun loadAlgorithms() {
        algorithms = repository.loadAlgorithms()
    }

    // Добавляем новый алгоритм
    fun addAlgorithm(algorithm: Algorithm) {
        algorithms = algorithms + algorithm
        repository.saveAlgorithms(algorithms)
    }
}

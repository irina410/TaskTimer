package com.example.tasktimer.viewmodel

import com.example.tasktimer.repository.AlgorithmRepository
import com.example.tasktimer.model.Algorithm

class AlgorithmViewModel(private val repository: AlgorithmRepository) {

    var algorithms: List<Algorithm> = emptyList()
        private set

    fun loadAlgorithms() {
        algorithms = repository.loadAlgorithms()
    }

    fun addAlgorithm(algorithm: Algorithm) {
        val updatedAlgorithms = algorithms.toMutableList()
        updatedAlgorithms.add(algorithm)
        algorithms = updatedAlgorithms
        repository.saveAlgorithms(algorithms)
    }

    fun removeAlgorithm(algorithm: Algorithm) {
        val updatedAlgorithms = algorithms.toMutableList()
        updatedAlgorithms.remove(algorithm)
        algorithms = updatedAlgorithms
        repository.saveAlgorithms(algorithms)
    }
}


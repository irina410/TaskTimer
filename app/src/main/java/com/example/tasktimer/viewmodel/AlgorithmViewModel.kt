package com.example.tasktimer.viewmodel

import com.example.tasktimer.repository.AlgorithmRepository
import com.example.tasktimer.model.Algorithm

class AlgorithmViewModel(private val repository: AlgorithmRepository) {

    var algorithms: List<Algorithm> = emptyList()
        private set // Закрываем сеттер, чтобы изменять могли только внутри класса

    // Загрузка алгоритмов из хранилища
    fun loadAlgorithms() {
        algorithms = repository.loadAlgorithms()
    }

    // Добавление нового алгоритма
    fun addAlgorithm(algorithm: Algorithm) {
        val updatedAlgorithms = algorithms.toMutableList()
        updatedAlgorithms.add(algorithm)
        algorithms = updatedAlgorithms
        repository.saveAlgorithms(algorithms) // Сохраняем изменения
    }


}

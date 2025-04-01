package com.example.tasktimer.model

// Класс данных для алгоритма
data class Algorithm(
    val name: String,
    val subtasks: List<Subtask>,
    var totalTime: Long = 0L // Время в секундах
) {
    fun recalculateTotalTime() {
        totalTime = subtasks.sumOf { it.duration }
    }
    fun copy() = Algorithm(name, subtasks.toList(), totalTime)
}


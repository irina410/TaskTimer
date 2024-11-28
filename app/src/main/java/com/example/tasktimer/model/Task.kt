package com.example.tasktimer.model

import com.example.tasktimer.model.Algorithm

data class Task(
    val number: Int,  // Уникальный номер задачи
    val algorithm: Algorithm,  // Алгоритм, с которым связана задача
    val isHighPriority: Boolean = false,  // Флаг высокой приоритетности задачи
    var isRunning: Boolean = false  // Статус выполнения задачи (запущена или нет)
) {
    // Метод для получения общего времени задачи (всего времени подзадач алгоритма)
    fun getTotalTime(): Long {
        return algorithm.totalTime
    }

    // Метод для получения названия алгоритма
    fun getAlgorithmName(): String {
        return algorithm.name
    }

    // Метод для обновления состояния задачи (например, для кнопки запуск/остановка)
    fun toggleRunningStatus() {
        isRunning = !isRunning
    }
}

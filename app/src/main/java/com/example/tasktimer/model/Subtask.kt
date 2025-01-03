package com.example.tasktimer.model

// Класс данных для подзадачи
data class Subtask(
    var description: String,
    var duration: Long, // Продолжительность в секундах
    var isHighPriority: Boolean = false,  // Флаг высокой приоритетности задачи
)
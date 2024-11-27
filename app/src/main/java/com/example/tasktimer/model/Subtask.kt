package com.example.tasktimer.model

// Класс данных для подзадачи
data class Subtask(
    val description: String,
    val duration: Long // Продолжительность в секундах
)
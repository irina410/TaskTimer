package com.example.tasktimer.model

data class Algorithm(
    val name: String,
    val subtasks: List<Subtask>,
    var totalTime: Long = 0L
) {
    fun recalculateTotalTime() {
        totalTime = subtasks.sumOf { it.duration }
    }

    fun copy() = Algorithm(name, subtasks.toList(), totalTime)
}


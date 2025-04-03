package com.example.tasktimer.model


data class Task(
    val number: Int,
    var algorithm: Algorithm,

    var isRunning: Boolean = false
) {
    fun getTotalTime(): Long {
        return algorithm.totalTime
    }

    fun getAlgorithmName(): String {
        return algorithm.name
    }

    fun toggleRunningStatus() {
        isRunning = !isRunning
    }
}

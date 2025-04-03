package com.example.tasktimer.repository

import android.content.Context
import com.example.tasktimer.model.Algorithm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AlgorithmRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("algorithm_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveAlgorithms(algorithms: List<Algorithm>) {
        val json = gson.toJson(algorithms)
        sharedPreferences.edit().putString("algorithms", json).apply()
    }

    fun loadAlgorithms(): List<Algorithm> {
        val json = sharedPreferences.getString("algorithms", null) ?: return emptyList()
        val type = object : TypeToken<List<Algorithm>>() {}.type //это создание анонимного класса,
        return gson.fromJson(json, type)
    }
}

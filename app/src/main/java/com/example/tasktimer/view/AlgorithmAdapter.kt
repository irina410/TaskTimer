package com.example.tasktimer.view


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm

// Адаптер для отображения списка алгоритмов
class AlgorithmAdapter : RecyclerView.Adapter<AlgorithmAdapter.AlgorithmViewHolder>() {

    // Список алгоритмов
    private var algorithms: List<Algorithm> = emptyList()

    // Метод для обновления данных
    fun submitList(newAlgorithms: List<Algorithm>) {
        algorithms = newAlgorithms
        notifyDataSetChanged() // Уведомляем RecyclerView об изменениях
    }

    // Создаём ViewHolder для одного элемента
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlgorithmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_algorithm, parent, false)
        return AlgorithmViewHolder(view)
    }

    // Привязываем данные к элементу
    override fun onBindViewHolder(holder: AlgorithmViewHolder, position: Int) {
        val algorithm = algorithms[position]
        holder.bind(algorithm)
    }

    // Возвращаем количество элементов
    override fun getItemCount(): Int = algorithms.size

    // ViewHolder для одного элемента списка
    class AlgorithmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.algorithmName)
        private val subtaskCountTextView: TextView = itemView.findViewById(R.id.subtaskCount)

        // Заполняем данные в UI
        fun bind(algorithm: Algorithm) {
            nameTextView.text = algorithm.name
            subtaskCountTextView.text = "Подзадач: ${algorithm.subtasks.size}"
        }
    }
}

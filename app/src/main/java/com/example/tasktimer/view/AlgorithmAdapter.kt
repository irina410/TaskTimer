package com.example.tasktimer.view

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm

class AlgorithmAdapter : RecyclerView.Adapter<AlgorithmAdapter.AlgorithmViewHolder>() {

    private var algorithms: List<Algorithm> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newAlgorithms: List<Algorithm>) {
        algorithms = newAlgorithms
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlgorithmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_algorithm, parent, false)
        return AlgorithmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlgorithmViewHolder, position: Int) {
        val algorithm = algorithms[position]
        holder.bind(algorithm)
    }

    override fun getItemCount(): Int = algorithms.size

    class AlgorithmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.algorithmName)

        fun bind(algorithm: Algorithm) {
            nameTextView.text = algorithm.name
        }
    }
}

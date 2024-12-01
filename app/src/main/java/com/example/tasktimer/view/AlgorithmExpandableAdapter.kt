import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm

class AlgorithmExpandableAdapter(
    private val algorithms: List<Algorithm>,
    private val onAlgorithmSelected: (Algorithm) -> Unit
) : RecyclerView.Adapter<AlgorithmExpandableAdapter.AlgorithmViewHolder>() {

    private var expandedAlgorithm: Algorithm? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlgorithmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_algorithm, parent, false)
        return AlgorithmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlgorithmViewHolder, position: Int) {
        val algorithm = algorithms[position]
        holder.bind(algorithm, isExpanded = algorithm == expandedAlgorithm)
    }

    override fun getItemCount() = algorithms.size

    inner class AlgorithmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val algorithmName: TextView = itemView.findViewById(R.id.algorithmName)
        private val subtasksContainer: LinearLayout = itemView.findViewById(R.id.subtasksContainer)
        private fun formatTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)

        }
        fun bind(algorithm: Algorithm, isExpanded: Boolean) {
            algorithmName.text = algorithm.name

            // Управление отображением подзадач
            subtasksContainer.removeAllViews()
            if (isExpanded) {
                subtasksContainer.visibility = View.VISIBLE
                algorithm.subtasks.forEach { subtask ->
                    val subtaskView = TextView(itemView.context).apply {
                        text = "${subtask.description} - ${formatTime(subtask.duration)}"
                        textSize = 14f
                    }
                    subtasksContainer.addView(subtaskView)
                }
            } else {
                subtasksContainer.visibility = View.GONE
            }

            // Обработка кликов
            algorithmName.setOnClickListener {
                expandedAlgorithm = if (isExpanded) null else algorithm
                notifyDataSetChanged()
            }

            itemView.setOnClickListener {
                onAlgorithmSelected(algorithm)
            }
        }
    }
}

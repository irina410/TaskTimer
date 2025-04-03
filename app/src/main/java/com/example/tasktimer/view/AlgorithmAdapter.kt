import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm

class AlgorithmAdapter(
    private var algorithms: List<Algorithm>,
    private val onAlgorithmSelected: (Algorithm) -> Unit
) : RecyclerView.Adapter<AlgorithmAdapter.AlgorithmViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlgorithmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_algorithm, parent, false)
        return AlgorithmViewHolder(view, onAlgorithmSelected)
    }

    override fun onBindViewHolder(holder: AlgorithmViewHolder, position: Int) {
        val algorithm = algorithms[position]
        holder.bind(algorithm)
    }

    override fun getItemCount(): Int = algorithms.size

    fun submitList(newAlgorithms: List<Algorithm>) {
        algorithms = newAlgorithms
        notifyDataSetChanged()
    }

    class AlgorithmViewHolder(
        itemView: View,
        private val onAlgorithmSelected: (Algorithm) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.algorithmName)
        private val totalTimeTextView: TextView = itemView.findViewById(R.id.algorithmTotalTime)

        fun bind(algorithm: Algorithm) {
            nameTextView.text = algorithm.name
            totalTimeTextView.text = formatSecondsToTime(algorithm.totalTime)

            itemView.setOnClickListener {
                onAlgorithmSelected(algorithm)
            }
        }

        private fun formatSecondsToTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
}

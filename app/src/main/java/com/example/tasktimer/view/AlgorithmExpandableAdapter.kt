import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.R
import com.example.tasktimer.model.Algorithm

class AlgorithmExpandableAdapter(
    private var algorithmList: List<Algorithm>,
    private val onAlgorithmSelected: (Algorithm) -> Unit
) : RecyclerView.Adapter<AlgorithmExpandableAdapter.AlgorithmViewHolder>() {

    // Метод для обновления данных
    fun updateData(newList: List<Algorithm>) {
        algorithmList = newList
        notifyDataSetChanged() // Обновляем адаптер
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlgorithmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_algorithm, parent, false)
        return AlgorithmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlgorithmViewHolder, position: Int) {
        val algorithm = algorithmList[position]
        holder.bind(algorithm)
    }

    override fun getItemCount(): Int {
        return algorithmList.size
    }

    inner class AlgorithmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val algorithmName: TextView = itemView.findViewById(R.id.algorithmName)

        fun bind(algorithm: Algorithm) {
            algorithmName.text = algorithm.name
            itemView.setOnClickListener {
                onAlgorithmSelected(algorithm)
            }
        }
    }
}

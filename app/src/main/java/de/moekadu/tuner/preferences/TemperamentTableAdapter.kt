package de.moekadu.tuner.preferences

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.RationalNumber
import kotlin.math.roundToInt

data class TemperamentTableEntry(val noteName: CharSequence, val cent: Float, val ratio: RationalNumber?, val stableId: Long)

class TemperamentTableEntryDiffCallback : DiffUtil.ItemCallback<TemperamentTableEntry>() {
    override fun areItemsTheSame(oldItem: TemperamentTableEntry, newItem: TemperamentTableEntry): Boolean {
        return oldItem.stableId == newItem.stableId
    }

    override fun areContentsTheSame(oldItem: TemperamentTableEntry, newItem: TemperamentTableEntry): Boolean {
        return oldItem == newItem
    }
}

class TemperamentTableAdapter : ListAdapter<TemperamentTableEntry, TemperamentTableAdapter.ViewHolder>(
    TemperamentTableEntryDiffCallback()
) {

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view){
        private var noteName: TextView? = null
        private var cent: TextView? = null
        private var numerator: TextView? = null
        private var denominator: TextView? = null
        var fractionSeparator: View?= null
        init {
            noteName = view.findViewById(R.id.note_name)
            cent = view.findViewById(R.id.cent)
            numerator = view.findViewById(R.id.ratio_numerator)
            denominator = view.findViewById(R.id.ratio_denominator)
            fractionSeparator = view.findViewById(R.id.ratio_separator)
        }

        fun setEntry(entry: TemperamentTableEntry) {
            noteName?.text = entry.noteName
            cent?.text = view.context.getString(R.string.cent_nosign, entry.cent.roundToInt())
            if (entry.ratio == null) {
                numerator?.visibility = View.GONE
                denominator?.visibility = View.GONE
                fractionSeparator?.visibility = View.GONE
            } else {
                numerator?.visibility = View.VISIBLE
                denominator?.visibility = View.VISIBLE
                fractionSeparator?.visibility = View.VISIBLE
                numerator?.text = entry.ratio.numerator.toString()
                denominator?.text = entry.ratio.denominator.toString()
            }
        }
    }

    fun setEntries(notes: Array<CharSequence>, cents: Array<Float>, ratios: Array<RationalNumber>?) {
        require(notes.size == cents.size)
        if (ratios != null) {
            require(notes.size == ratios.size)
        }

        val entries =  ArrayList<TemperamentTableEntry>()
        for (i in notes.indices)
            entries.add(TemperamentTableEntry(notes[i], cents[i], ratios?.get(i), i.toLong()))

        submitList(entries)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.temperament_table_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setEntry(getItem(position))
    }
}

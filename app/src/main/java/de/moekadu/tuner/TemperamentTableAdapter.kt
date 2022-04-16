package de.moekadu.tuner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

data class TemperamentTableEntry(val noteName: CharSequence, val cent: Float, val ratio: RationalNumber?)

class TemperamentTableEntryDiffCallback : DiffUtil.ItemCallback<TemperamentTableEntry>() {
    override fun areItemsTheSame(oldItem: TemperamentTableEntry, newItem: TemperamentTableEntry): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: TemperamentTableEntry, newItem: TemperamentTableEntry): Boolean {
        return oldItem == newItem
    }
}

class TemperamentTableAdapter : ListAdapter<TemperamentTableEntry, TemperamentTableAdapter.ViewHolder>(TemperamentTableEntryDiffCallback()) {

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view){
        var noteName: TextView? = null
        var cent: TextView? = null
    }

    fun setEntries(notes: Array<CharSequence>, cents: Array<Float>, ratios: Array<RationalNumber>?) {
        require(notes.size == cents.size)
        if (ratios != null) {
            require(notes.size == ratios.size)
        }

        val entries =  ArrayList<TemperamentTableEntry>()
        for (i in notes.indices)
            entries.add(TemperamentTableEntry(notes[i], cents[i], ratios?.get(i)))

        submitList(entries)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.temperament_table_entry, parent, false)
        return ViewHolder(view).apply {
            noteName = view.findViewById(R.id.note_name)
            cent = view.findViewById(R.id.cent)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.noteName?.text = entry.noteName
        holder.cent?.text = holder.view.context.getString(R.string.cent_nosign, entry.cent.roundToInt())
    }
}

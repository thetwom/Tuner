package de.moekadu.tuner

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView

class InstrumentsSectionAdapter(private val sectionResourceId: Int)
    : RecyclerView.Adapter<InstrumentsSectionAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    fun interface SectionClickedListener {
        fun onSectionClicked()
    }

    var sectionClickedListener: SectionClickedListener? = null

    private var section: TextView? = null
    private var icon: AppCompatImageView? = null

    var expanded = true
        set(value) {
            if (value != field) {
//                Log.v("Tuner", "InstrumentSectionAdapter.expanded = $expanded")
                field = value
                icon?.setImageResource(if (value) R.drawable.ic_expand else R.drawable.ic_collapsed)
            }
        }
    //var sectionClickedListener: SectionClickedListener? = null
    var visible = true
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return Long.MAX_VALUE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.instrument_entry_section, parent, false)
        section = view.findViewById(R.id.section_title)
        icon = view.findViewById(R.id.expand_collapse)
        return ViewHolder(view).apply {
            view.setOnClickListener {
                sectionClickedListener?.onSectionClicked()
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        section?.setText(sectionResourceId)
        icon?.setImageResource(if (expanded) R.drawable.ic_expand else R.drawable.ic_collapsed)
    }

    override fun getItemCount(): Int {
        return if (visible) 1 else 0
    }
}
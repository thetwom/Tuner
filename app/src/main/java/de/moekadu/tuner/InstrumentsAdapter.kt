package de.moekadu.tuner

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class InstrumentDiffCallback : DiffUtil.ItemCallback<Instrument>() {
    override fun areItemsTheSame(oldItem: Instrument, newItem: Instrument): Boolean {
        return oldItem.stableId == newItem.stableId
    }

    override fun areContentsTheSame(oldItem: Instrument, newItem: Instrument): Boolean {
        return oldItem == newItem
    }
}

class InstrumentsAdapter : ListAdapter<Instrument, InstrumentsAdapter.ViewHolder>(InstrumentDiffCallback()) {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var titleView: TextView? = null
        var icon: ImageView? = null
        var selectedView: View? = null
        var instrument: Instrument? = null

        var isActivated = false
            set(value) {
                field = value
                if (value) {
                    selectedView?.visibility = View.VISIBLE
                }
                else {
                    selectedView?.visibility = View.INVISIBLE
                }
            }
    }

    fun interface OnInstrumentClickedListener {
        fun onInstrumentClicked(instrument: Instrument, stableId: Long)
    }

    private var activatedStableId = Instrument.NO_STABLE_ID
    var onInstrumentClickedListener: OnInstrumentClickedListener? = null

    init {
        setHasStableIds(true)
    }

    override fun submitList(list: List<Instrument>?) {
        Log.v("Tuner", "InstrumentsAdapter.submitList: size=${list?.size}, list=$list")
        super.submitList(list)
    }

    override fun getItemId(position: Int): Long {
//        Log.v("Tuner", "InstrumentAdapter.getItemId: position=$position, itemId=${getItem(position).stableId}")
        return getItem(position).stableId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.instrument_entry, parent, false)
        return ViewHolder(view).apply {
            titleView = view.findViewById(R.id.instrument_title)
            icon = view.findViewById(R.id.instrument_icon)
            selectedView = view.findViewById(R.id.instrument_active)
            view.setOnClickListener {
                activatedStableId = itemId
                onInstrumentClickedListener?.onInstrumentClicked(getItem(bindingAdapterPosition), itemId)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val instrument = getItem(position)
        holder.titleView?.text = instrument.getNameString(holder.view.context)
        holder.icon?.setImageResource(instrument.iconResource)
        holder.isActivated = (instrument.stableId == activatedStableId)
        holder.instrument = instrument
    }

    fun setActiveStableId(stableId: Long, recyclerView: RecyclerView?) {
        activatedStableId = stableId
//        Log.v("Tuner", "InstrumentsAdapter.setActiveStableId: stableId=$stableId")
        if (recyclerView != null) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val viewHolder = recyclerView.getChildViewHolder(child)
                if (viewHolder is InstrumentsAdapter.ViewHolder) {
                    viewHolder.isActivated = (stableId == viewHolder.itemId)
                }
            }
        }
    }
}
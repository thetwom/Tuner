package de.moekadu.tuner

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

fun RecyclerView.forEachViewHolder(op: (RecyclerView.ViewHolder) -> Unit) {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        getChildViewHolder(child)?.let { viewHolder -> op(viewHolder) }
    }
}

class InstrumentsAdapter : ListAdapter<Instrument, InstrumentsAdapter.ViewHolder>(InstrumentDiffCallback()) {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var titleView: TextView? = null
        var icon: ImageView? = null
        var stringText: TextView? = null

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

    private var tuningFrequencies: TuningFrequencies? = null

    private var activatedStableId = Instrument.NO_STABLE_ID
    var onInstrumentClickedListener: OnInstrumentClickedListener? = null

    init {
        setHasStableIds(true)
    }

    fun setTuningFrequencies(tuningFrequencies: TuningFrequencies?, recyclerView: RecyclerView?) {
        this.tuningFrequencies = tuningFrequencies
        if (recyclerView == null || tuningFrequencies == null)
            return

        recyclerView.forEachViewHolder { holder ->
            if (holder is ViewHolder) {
                val position = holder.bindingAdapterPosition
                if (position >= 0) {
                    val instrument = getItem(position)
                    holder.stringText?.text = instrument.getStringsString(holder.view.context, tuningFrequencies, preferFlat = false)
                }
            }
        }
    }
//    override fun submitList(list: List<Instrument>?) {
//        Log.v("Tuner", "InstrumentsAdapter.submitList: size=${list?.size}, list=$list")
//        super.submitList(list)
//    }

    override fun getItemId(position: Int): Long {
//        Log.v("Tuner", "InstrumentAdapter.getItemId: position=$position, itemId=${getItem(position).stableId}")
        return getItem(position).stableId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.instrument_entry, parent, false)
        return ViewHolder(view).apply {
            titleView = view.findViewById(R.id.instrument_title)
            stringText = view.findViewById(R.id.string_list)
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
        tuningFrequencies?.let {
            holder.stringText?.text = instrument.getStringsString(
                holder.view.context,
                it,
                preferFlat = false
            )
        }
        holder.icon?.setImageResource(instrument.iconResource)
        holder.isActivated = (instrument.stableId == activatedStableId)
        holder.instrument = instrument
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        holder.isActivated = (holder.itemId == activatedStableId)
        val position = holder.bindingAdapterPosition
        val instrument = getItem(position)
        holder.titleView?.text = instrument.getNameString(holder.view.context)
        tuningFrequencies?.let {
            holder.stringText?.text = instrument.getStringsString(
                holder.view.context,
                it,
                preferFlat = false
            )
        }
        holder.icon?.setImageResource(instrument.iconResource)
        holder.isActivated = (instrument.stableId == activatedStableId)
        holder.instrument = instrument
        super.onViewAttachedToWindow(holder)
    }

    fun setActiveStableId(stableId: Long, recyclerView: RecyclerView?) {
        activatedStableId = stableId
//        Log.v("Tuner", "InstrumentsAdapter.setActiveStableId: stableId=$stableId")
        if (recyclerView != null) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val viewHolder = recyclerView.getChildViewHolder(child)
                if (viewHolder is ViewHolder) {
                    viewHolder.isActivated = (stableId == viewHolder.itemId)
                }
            }
        }
    }
}
/*
 * Copyright 2019 Michael Moessner
 *
 * This file is part of Metronome.
 *
 * Metronome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metronome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Metronome.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.moekadu.tuner.dialogs

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument

class InstrumentToBeShared(val instrument: Instrument, var isShared: Boolean)

class InstrumentsSharingDialogAdapter(instruments: List<Instrument>?) : RecyclerView.Adapter<InstrumentsSharingDialogAdapter.ViewHolder>() {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private var titleView = view.findViewById<TextView>(R.id.instrument_title)
        private var checkBox = view.findViewById<CheckBox>(R.id.checkbox).apply {
            isClickable = false
        }

        fun setInstrument(instrumentToBeShared: InstrumentToBeShared, position: Int) {
            val titleViewText = instrumentToBeShared.instrument.getNameString(view.context)
            titleView?.text = titleViewText
            checkBox?.isChecked = instrumentToBeShared.isShared
            titleView?.typeface = Typeface.DEFAULT
        }
        fun setCheckAll(resourceId: Int, isChecked: Boolean) {
            titleView?.setText(resourceId)
            checkBox?.isChecked = isChecked
            titleView?.typeface = Typeface.DEFAULT_BOLD
        }
    }

    private val instrumentsWhichCanBeShared = Array(instruments?.size ?: 0) {
            InstrumentToBeShared(instruments!![it], true)
    }

    init {
        setHasStableIds(true)
    }

    fun getInstrumentsToBeShared(): List<Instrument> {
        return instrumentsWhichCanBeShared.filter { it.isShared }.map { it.instrument }
    }

    fun getStateOfEachInstrument(): List<Boolean> {
        return instrumentsWhichCanBeShared.map { it.isShared }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setStateOfEachInstrument(states: List<Boolean>) {
        for (i in instrumentsWhichCanBeShared.indices) {
            if (i in states.indices)
                instrumentsWhichCanBeShared[i].isShared = states[i]
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return instrumentsWhichCanBeShared.size + 1 // first is checkAll
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.checkbox_instrument, parent, false)
        return ViewHolder(view).apply {
            view.setOnClickListener {
                if (itemId == 0L) {
                    val allChecked = instrumentsWhichCanBeShared.all { it.isShared }
                    if (allChecked)
                        instrumentsWhichCanBeShared.forEach { it.isShared = false }
                    else
                        instrumentsWhichCanBeShared.forEach { it.isShared = true }
                    notifyDataSetChanged()
                } else {
                    val isShared = instrumentsWhichCanBeShared[itemId.toInt()-1].isShared
                    instrumentsWhichCanBeShared[itemId.toInt()-1].isShared = !isShared
                    notifyItemChanged(itemId.toInt())
                    notifyItemChanged(0)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            val allChecked = instrumentsWhichCanBeShared.all { it.isShared }
            holder.setCheckAll(R.string.select_all, allChecked)
        } else {
            holder.setInstrument(instrumentsWhichCanBeShared[position - 1], position - 1)
        }
    }
}

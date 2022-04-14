package de.moekadu.tuner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

data class TemperamentProperties(val tuning: Tuning, val nameId: Int, val descriptionId: Int?)

class TemperamentSpinnerAdapter(context: Context) : ArrayAdapter<TemperamentProperties>(context, 0){
    private val inflater = LayoutInflater.from(context)
    private val tunings = Tuning.values()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.temperament_spinner_item, parent, false)
        val title = view.findViewById<TextView>(R.id.temperament_name)
        val description = view.findViewById<TextView>(R.id.temperament_description)
        val separator = view.findViewById<View>(R.id.seperator)
        separator?.visibility = View.GONE
        title.setText(getTuningNameResourceId(tunings[position]))
        val descId = getTuningDescriptionResourceId(tunings[position])
        if (descId == null)
            description.text = ""
        else
            description.setText(descId)

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent).apply {
            findViewById<View>(R.id.seperator)?.visibility = View.VISIBLE
        }
    }

    override fun getCount(): Int {
        return tunings.size
    }

    override fun getItem(position: Int): TemperamentProperties? {
        if (position >= tunings.size)
            return null
        val tuning = tunings[position]
        return TemperamentProperties(tuning, getTuningNameResourceId(tuning), getTuningDescriptionResourceId(tuning))
    }
}
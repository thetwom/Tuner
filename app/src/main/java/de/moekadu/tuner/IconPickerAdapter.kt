package de.moekadu.tuner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.widget.AppCompatImageView

class IconPickerAdapter(private val icons: IntArray) : BaseAdapter() {
    override fun getCount(): Int {
        return icons.size
    }

    override fun getItem(position: Int): Any {
        return icons[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(parent!!.context).inflate(R.layout.icon_picker_entry, parent, false)
        val icon: AppCompatImageView = view.findViewById(R.id.icon)
        icon.setImageResource(icons[position])
        return view
    }

}

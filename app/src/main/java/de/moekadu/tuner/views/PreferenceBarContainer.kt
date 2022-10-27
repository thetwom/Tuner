package de.moekadu.tuner.views

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.*

class PreferenceBarContainer(val activity: MainActivity) {
    private val background = activity.findViewById<View>(R.id.properties_bar)
    private val referenceNote = activity.findViewById<TextView>(R.id.reference_note)
    private val temperament = activity.findViewById<TextView>(R.id.temperament)
    private val preferFlatSwitch = activity.findViewById<ImageView>(R.id.prefer_flat)

    var visibility = View.VISIBLE
        set(value) {
            background.visibility = value
            referenceNote.visibility = value
            temperament.visibility = value
            preferFlatSwitch.visibility = value
            field = value
        }

    var preferFlat = false
        set(value) {
            if (value)
                preferFlatSwitch.setImageResource(R.drawable.ic_prefer_flat_isflat)
            else
                preferFlatSwitch.setImageResource(R.drawable.ic_prefer_flat_issharp)
            field = value
        }

    init {
        preferFlatSwitch.setOnClickListener {
            activity.switchEnharmonicSetting()
        }

        referenceNote.setOnClickListener {
            activity.showReferenceNoteDialog()
        }

        temperament.setOnClickListener {
            activity.showTemperamentDialog()
        }
    }

    fun setReferenceNote(note: MusicalNote, frequency: String, printOption: MusicalNotePrintOptions) {
        val printer = NoteNamePrinter(activity)
        val builder = SpannableStringBuilder()
        builder.append(printer.noteToCharSequence(note, printOption, true))
        builder.append("\n")
        builder.append(activity.getString(R.string.hertz_str, frequency)) // TODO: instead use the settings string
        referenceNote.text = builder
    }

    fun setTemperament(temperamentType: TemperamentType) {
        temperament.text = activity.getString(getTuningNameAbbrResourceId(temperamentType))
    }
}
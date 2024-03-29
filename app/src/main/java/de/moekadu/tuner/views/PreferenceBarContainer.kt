package de.moekadu.tuner.views

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import de.moekadu.tuner.MainActivity
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteNamePrinter
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.temperaments.getTuningNameAbbrResourceId

class PreferenceBarContainer(val activity: MainActivity) {
    private val background = activity.findViewById<View>(R.id.properties_bar)
    private val referenceNote = activity.findViewById<TextView>(R.id.reference_note)
    private val temperament = activity.findViewById<TextView>(R.id.temperament)
    private val preferFlatSwitch = activity.findViewById<ImageView>(R.id.prefer_flat)

    private var isPreferFlatSwitchVisible = true
    var visibility = View.VISIBLE
        set(value) {
            background.visibility = value
            referenceNote.visibility = value
            temperament.visibility = value
            preferFlatSwitch.visibility = if (isPreferFlatSwitchVisible)
                value
            else
                View.INVISIBLE
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

    fun setReferenceNote(note: MusicalNote, frequency: String, noteNamePrinter: NoteNamePrinter) {
        val builder = SpannableStringBuilder()
        builder.append(noteNamePrinter.noteToCharSequence(note, true))
        builder.append("\n")
        builder.append(activity.getString(R.string.hertz_str, frequency))
        referenceNote.text = builder
    }

    fun setTemperament(temperamentType: TemperamentType) {
        temperament.text = activity.getString(getTuningNameAbbrResourceId(temperamentType))
    }

    fun setSharpFlatPreferenceVisibility(isVisible: Boolean) {
//        Log.v("Tuner", "PreferenceBarContainer.setSharpFlatPreferenceVisibility: isVisible=$isVisible")
        isPreferFlatSwitchVisible = isVisible
        if (visibility == View.VISIBLE)
            preferFlatSwitch.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }
}
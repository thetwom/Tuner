package de.moekadu.tuner.preferences

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.*
import kotlin.math.absoluteValue

private fun fifthCorrectionString(context: Context, correction: FifthModification): String {
    val s = StringBuilder()

    var r = correction.pythagoreanComma
    if (!r.isZero) {
        if (s.isNotEmpty()) {
            if (r.numerator >= 0)
                s.append(context.getString(R.string.plus_correction))
            else
                s.append(context.getString(R.string.minus_correction))
        } else if (r.numerator < 0) {
            s.append("-")
        }
        s.append(
            context.getString(R.string.pythagorean_comma, r.numerator.absoluteValue, r.denominator)
        )
    }

    r = correction.syntonicComma
    if (!r.isZero) {
        if (s.isNotEmpty()) {
            if (r.numerator >= 0)
                s.append(context.getString(R.string.plus_correction))
            else
                s.append(context.getString(R.string.minus_correction))
        } else if (r.numerator < 0) {
            s.append("-")
        }
        s.append(
            context.getString(R.string.syntonic_comma, r.numerator.absoluteValue, r.denominator)
        )
    }

    r = correction.schisma
    if (!r.isZero) {
        if (s.isNotEmpty()) {
            if (r.numerator >= 0)
                s.append(context.getString(R.string.plus_correction))
            else
                s.append(context.getString(R.string.minus_correction))
        } else if (r.numerator < 0) {
            s.append("-")
        }
        s.append(
            context.getString(R.string.schisma, r.numerator.absoluteValue, r.denominator)
        )
    }

    return s.toString()
}

data class TemperamentCircleOfFifthsEntry(var note: MusicalNote?, var fifthsModification: FifthModification?, val stableId: Long)

class TemperamentCircleOfFifthsEntryDiffCallback: DiffUtil.ItemCallback<TemperamentCircleOfFifthsEntry>() {
    override fun areItemsTheSame(oldItem: TemperamentCircleOfFifthsEntry, newItem: TemperamentCircleOfFifthsEntry): Boolean {
        return oldItem.stableId == newItem.stableId
    }

    override fun areContentsTheSame(oldItem: TemperamentCircleOfFifthsEntry, newItem: TemperamentCircleOfFifthsEntry): Boolean {
        return oldItem.note == newItem.note && oldItem.fifthsModification == newItem.fifthsModification
    }
}

class TemperamentCircleOfFifthsAdapter
    : ListAdapter<TemperamentCircleOfFifthsEntry, TemperamentCircleOfFifthsAdapter.ViewHolder>(
    TemperamentCircleOfFifthsEntryDiffCallback()
) {

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view){
        private var noteName: TextView? = null
        private var arrowStroke: ImageView? = null
        private var arrowHead: ImageView? = null
        private var fifthsModification: TextView? = null

        init {
            noteName = view.findViewById(R.id.note_name)
            arrowStroke = view.findViewById(R.id.arrow_stroke)
            arrowHead= view.findViewById(R.id.arrow_head)
            fifthsModification = view.findViewById(R.id.fifths_modification)
        }

        fun setEntry(entry: TemperamentCircleOfFifthsEntry, noteNamePrinter: NoteNamePrinter) {
            val note = entry.note
            val circleOfFifths = entry.fifthsModification
            if (note != null) {
                noteName?.visibility = View.VISIBLE
                arrowStroke?.visibility = View.GONE
                arrowHead?.visibility = View.GONE
                fifthsModification?.visibility = View.GONE
                noteName?.text = noteNamePrinter.noteToCharSequence(note, withOctave = false)
            } else if (circleOfFifths != null){
                noteName?.visibility = View.GONE
                arrowStroke?.visibility = View.VISIBLE
                arrowHead?.visibility = View.VISIBLE
                fifthsModification?.visibility = View.VISIBLE
                fifthsModification?.text = fifthCorrectionString(view.context, circleOfFifths)
            }
        }
    }

    private val entries = Array(25) {
        TemperamentCircleOfFifthsEntry(null, null, it.toLong())
    }

    private var noteNamePrinter: NoteNamePrinter? = null

    fun setEntries(
        rootNote: MusicalNote?, noteNameScale: NoteNameScale?,
        circleOfFifths: TemperamentCircleOfFifths?, noteNamePrinter: NoteNamePrinter
    ) {
        this.noteNamePrinter = noteNamePrinter
        if (rootNote != null && noteNameScale != null) {
            val rootNoteIndex = noteNameScale.getIndexOfNote(rootNote)
//        Log.v("Tuner", "TemperamentCircleOfFifthsAdapter.setEntries: rootNoteIndex=$rootNoteIndex")
            var noteIndex = rootNoteIndex
            for (i in 0..12) {
//                Log.v("Tuner", "TemperamentCircleOfFifthsAdapter.setEntries: i=$i, noteIndex=$noteIndex")
                entries[2 * i].note = noteNameScale.getNoteOfIndex(noteIndex)
                noteIndex += 7
            }
        }

        if (circleOfFifths != null) {
            entries[1].fifthsModification = circleOfFifths.CG
            entries[3].fifthsModification = circleOfFifths.GD
            entries[5].fifthsModification = circleOfFifths.DA
            entries[7].fifthsModification = circleOfFifths.AE
            entries[9].fifthsModification = circleOfFifths.EB
            entries[11].fifthsModification = circleOfFifths.BFsharp
            entries[13].fifthsModification = circleOfFifths.FsharpCsharp
            entries[15].fifthsModification = circleOfFifths.CsharpGsharp
            entries[17].fifthsModification = circleOfFifths.GsharpEflat
            entries[19].fifthsModification = circleOfFifths.EFlatBflat
            entries[21].fifthsModification = circleOfFifths.BflatF
            entries[23].fifthsModification = circleOfFifths.FC
        }
        val entriesCopy = ArrayList<TemperamentCircleOfFifthsEntry>()
        entries.forEach { entriesCopy.add(it.copy()) }
        submitList(entriesCopy)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.temperament_circle_of_fifths_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        noteNamePrinter?.let { printer ->
            holder.setEntry(getItem(position), printer)
        }
    }
}

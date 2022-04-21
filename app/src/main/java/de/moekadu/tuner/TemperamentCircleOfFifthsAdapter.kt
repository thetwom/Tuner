package de.moekadu.tuner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

data class TemperamentCircleOfFifthsEntry(var noteIndex: Int?, var fifthsModification: FifthModification?, var preferFlat: Boolean, val stableId: Long)

class TemperamentCircleOfFifthsEntryDiffCallback: DiffUtil.ItemCallback<TemperamentCircleOfFifthsEntry>() {
    override fun areItemsTheSame(oldItem: TemperamentCircleOfFifthsEntry, newItem: TemperamentCircleOfFifthsEntry): Boolean {
        return oldItem.stableId == newItem.stableId
    }

    override fun areContentsTheSame(oldItem: TemperamentCircleOfFifthsEntry, newItem: TemperamentCircleOfFifthsEntry): Boolean {
        return oldItem == newItem
    }
}

class TemperamentCircleOfFifthsAdapter
    : ListAdapter<TemperamentCircleOfFifthsEntry, TemperamentCircleOfFifthsAdapter.ViewHolder>(TemperamentCircleOfFifthsEntryDiffCallback()) {

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

        fun setEntry(entry: TemperamentCircleOfFifthsEntry) {
            val noteIndex = entry.noteIndex
            val circleOfFifths = entry.fifthsModification
            if (noteIndex != null) {
                noteName?.visibility = View.VISIBLE
                arrowStroke?.visibility = View.GONE
                arrowHead?.visibility = View.GONE
                fifthsModification?.visibility = View.GONE
                noteName?.text = noteNames12Tone.getNoteName(view.context, noteIndex, entry.preferFlat, false)
            } else if (circleOfFifths != null){
                noteName?.visibility = View.GONE
                arrowStroke?.visibility = View.VISIBLE
                arrowHead?.visibility = View.VISIBLE
                fifthsModification?.visibility = View.VISIBLE
                fifthsModification?.text = fifthCorrectionString(view.context, circleOfFifths)
            }
        }
    }

    private val entries =  Array(25) {
        TemperamentCircleOfFifthsEntry(null, null, preferFlat = false, it.toLong())
    }

    fun setEntries(rootNoteIndex: Int?, circleOfFifths: TuningCircleOfFifths?, preferFlat: Boolean) {
        entries.forEach { it.preferFlat = preferFlat }
//        Log.v("Tuner", "TemperamentCircleOfFifthsAdapter.setEntries: rootNoteIndex=$rootNoteIndex")
        if (rootNoteIndex != null) {
            var noteIndex = rootNoteIndex
            for (i in 0 .. 12) {
//                Log.v("Tuner", "TemperamentCircleOfFifthsAdapter.setEntries: i=$i, noteIndex=$noteIndex")
                entries[2 * i].noteIndex = noteIndex
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
        holder.setEntry(getItem(position))
    }
}

package de.moekadu.tuner.instruments

import android.content.Context
import android.os.Parcelable
import android.text.SpannableStringBuilder
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.NoteNamePrinter
import kotlinx.parcelize.Parcelize

@Parcelize
data class Instrument(private val name: CharSequence?, private val nameResource: Int?, val strings: Array<MusicalNote>,
                      val iconResource: Int, val stableId: Long, val isChromatic: Boolean = false) :
    Parcelable {
    //val stringsSorted = strings.map { it.toFloat() }.toFloatArray().sortedArray()

    /** Get instrument name as a char sequence.
     * @param context Context is only needed, if the instrument name is a string resource.
     * @return Name of instrument.
     */
    fun getNameString(context: Context?): CharSequence {
        return when {
            nameResource != null && context != null -> context.getString(nameResource)
            name != null -> name
            else -> throw RuntimeException("No name given for instrument")
        }
    }

    /** Get readable representation of all strings (e.g. "A#4 - C5 - G5")
     * @param context Context for obtaining string resources.
     * @param noteNamePrinter Transfer notes to char sequences.
     */
    fun getStringsString(context: Context, noteNamePrinter: NoteNamePrinter): CharSequence {
        return if (isChromatic) {
            context.getString(R.string.chromatic)
        } else {
            val builder = SpannableStringBuilder()
//            Log.v("Tuner", "Instrument.getStringsString: printOption=$printOption, preferFlat=$preferFlat")
            if (strings.isNotEmpty())
                builder.append(noteNamePrinter.noteToCharSequence(strings[0], withOctave = true))
            for (i in 1 until strings.size) {
                builder.append(" - ")
                builder.append(noteNamePrinter.noteToCharSequence(strings[i], withOctave = true))
            }
            builder
//            strings.joinToString(" - ", "", "") {
//                tuningFrequencies.getNoteName(context, it, preferFlat)
//            }
        }
    }

    companion object {
        const val NO_STABLE_ID = Long.MAX_VALUE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Instrument

        if (name != other.name) return false
        if (nameResource != other.nameResource) return false
        if (!strings.contentEquals(other.strings)) return false
        if (iconResource != other.iconResource) return false
        if (stableId != other.stableId) return false
        if (isChromatic != other.isChromatic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (nameResource ?: 0)
        result = 31 * result + strings.contentHashCode()
        result = 31 * result + iconResource
        result = 31 * result + stableId.hashCode()
        result = 31 * result + isChromatic.hashCode()
        return result
    }
}

val instrumentChromatic = Instrument(
    name = null,
    nameResource = R.string.chromatic,
    strings = arrayOf(),
    iconResource = R.drawable.ic_piano,
    stableId = -1, // this should be set by a id generator
    isChromatic = true
)

val instrumentDatabase = createInstrumentDatabase()

private fun createInstrumentDatabase(): ArrayList<Instrument> {
    val instruments = ArrayList<Instrument>()
    instruments.add(
        instrumentChromatic.copy(stableId = -1 - instruments.size.toLong())
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.guitar_eadgbe,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 2),
                MusicalNote(BaseNote.A, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.B, NoteModifier.None, 3),
                MusicalNote(BaseNote.E, NoteModifier.None, 4)
            ),
            iconResource = R.drawable.ic_guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.bass_eadg,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 1),
                MusicalNote(BaseNote.A, NoteModifier.None, 1),
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
            ),
            iconResource = R.drawable.ic_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.bass_beadg,
            strings = arrayOf(
                MusicalNote(BaseNote.B, NoteModifier.None, 0),
                MusicalNote(BaseNote.E, NoteModifier.None, 1),
                MusicalNote(BaseNote.A, NoteModifier.None, 1),
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
            ),
            iconResource = R.drawable.ic_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.ukulele_gcea,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 4),
                MusicalNote(BaseNote.C, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 4),
                MusicalNote(BaseNote.A, NoteModifier.None, 4)
            ),
            iconResource = R.drawable.ic_ukulele,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.violin_gdae,
            strings = arrayOf(
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4),
                MusicalNote(BaseNote.A, NoteModifier.None, 4),
                MusicalNote(BaseNote.E, NoteModifier.None, 5),
            ),
            iconResource = R.drawable.ic_violin,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.viola_cgda,
            strings = arrayOf(
                MusicalNote(BaseNote.C, NoteModifier.None, 3),
                MusicalNote(BaseNote.G, NoteModifier.None, 3),
                MusicalNote(BaseNote.D, NoteModifier.None, 4),
                MusicalNote(BaseNote.A, NoteModifier.None, 4),
            ),
            iconResource = R.drawable.ic_violin,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.cello_cgda,
            strings = arrayOf(
                MusicalNote(BaseNote.C, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
                MusicalNote(BaseNote.D, NoteModifier.None, 3),
                MusicalNote(BaseNote.A, NoteModifier.None, 3),
            ),
            iconResource = R.drawable.ic_cello,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.double_bass_eadg,
            strings = arrayOf(
                MusicalNote(BaseNote.E, NoteModifier.None, 1),
                MusicalNote(BaseNote.A, NoteModifier.None, 1),
                MusicalNote(BaseNote.D, NoteModifier.None, 2),
                MusicalNote(BaseNote.G, NoteModifier.None, 2),
            ),
            iconResource = R.drawable.ic_double_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
//    instruments.add(
//        Instrument(
//            name = "Test instrument",
//            nameResource = null,
//            strings = arrayOf(
//                MusicalNote(BaseNote.A, NoteModifier.None, 4),
//                MusicalNote(BaseNote.A, NoteModifier.Sharp, 4, BaseNote.B, NoteModifier.Flat),
//                MusicalNote(BaseNote.B, NoteModifier.None, 4),
//                MusicalNote(BaseNote.A, NoteModifier.None, 4),
//                MusicalNote(BaseNote.B, NoteModifier.None, 4),
//                MusicalNote(BaseNote.B, NoteModifier.None, 4),
//                MusicalNote(BaseNote.C, NoteModifier.None, 5),
//                MusicalNote(BaseNote.C, NoteModifier.Sharp, 5, BaseNote.D, NoteModifier.Flat),
//                MusicalNote(BaseNote.D, NoteModifier.None, 5),
//                MusicalNote(BaseNote.A, NoteModifier.None, 4)
//            ),
//            iconResource = R.drawable.ic_violin,
//            stableId = -1 - instruments.size.toLong()
//        )
//    )
//    instruments.add(
//        Instrument(
//            name = "Test instrument 2",
//            nameResource = null,
//            strings = arrayOf(
//                MusicalNote(BaseNote.A, NoteModifier.None, 4),
//                MusicalNote(BaseNote.A, NoteModifier.Sharp, 4, BaseNote.B, NoteModifier.Flat),
//                MusicalNote(BaseNote.A, NoteModifier.Sharp, 4, BaseNote.B, NoteModifier.Flat),
//            iconResource = R.drawable.ic_violin,
//            stableId = -1 - instruments.size.toLong()
//        )
//    )
    return instruments
}

package de.moekadu.tuner.instruments

import android.content.Context
import android.text.SpannableStringBuilder
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.NoteNames

data class Instrument(private val name: CharSequence?, private val nameResource: Int?, val strings: IntArray,
                      val iconResource: Int, val stableId: Long, val isChromatic: Boolean = false) {
    val stringsSorted = strings.map { it.toFloat() }.toFloatArray().sortedArray()

    fun getNameString(context: Context?): CharSequence {
        return when {
            nameResource != null && context != null -> context.getString(nameResource)
            name != null -> name
            else -> throw RuntimeException("No name given for instrument")
        }
    }

    fun getStringsString(context: Context, noteNames: NoteNames, preferFlat: Boolean): CharSequence {
        return if (isChromatic) {
            context.getString(R.string.chromatic)
        } else {
            val builder = SpannableStringBuilder()
            if (strings.isNotEmpty())
                builder.append(noteNames.getNoteName(context, strings[0], preferFlat))
            for (i in 1 until strings.size) {
                builder.append(" - ")
                builder.append(noteNames.getNoteName(context, strings[i], preferFlat))
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
        if (!strings.contentEquals(other.strings)) return false
        if (iconResource != other.iconResource) return false
        if (stableId != other.stableId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + strings.contentHashCode()
        result = 31 * result + iconResource
        result = 31 * result + stableId.hashCode()
        return result
    }
}

val instrumentDatabase = createInstrumentDatabase()

private fun createInstrumentDatabase(): ArrayList<Instrument> {
    val instruments = ArrayList<Instrument>()
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.chromatic,
            strings = intArrayOf(),
            iconResource = R.drawable.ic_piano,
            stableId = -1 - instruments.size.toLong(), // this should be set by a id generator
            isChromatic = true
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.guitar_eadgbe,
            strings = intArrayOf(-29, -24, -19, -14, -10, -5),
            iconResource = R.drawable.ic_guitar,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.bass_eadg,
            strings = intArrayOf(-41, -36, -31, -26),
            iconResource = R.drawable.ic_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.bass_beadg,
            strings = intArrayOf(-46, -41, -36, -31, -26),
            iconResource = R.drawable.ic_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.ukulele_gcea,
            strings = intArrayOf(-2, -9, -5, 0),
            iconResource = R.drawable.ic_ukulele,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.violin_gdae,
            strings = intArrayOf(-14, -7, 0, 7),
            iconResource = R.drawable.ic_violin,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.viola_cgda,
            strings = intArrayOf(-21, -14, -7, 0),
            iconResource = R.drawable.ic_violin,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.cello_cgda,
            strings = intArrayOf(-33, -26, -19, -12),
            iconResource = R.drawable.ic_cello,
            stableId = -1 - instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.double_bass_eadg,
            strings = intArrayOf(-41, -36, -31, -26),
            iconResource = R.drawable.ic_double_bass,
            stableId = -1 - instruments.size.toLong()
        )
    )
//    instruments.add(
//        Instrument(
//            name = "Test instrument",
//            nameResource = null,
//            strings = intArrayOf(0, 1, 2, 0, 2, 2, 3, 4, 5, 0),
//            iconResource = R.drawable.ic_violin,
//            stableId = -1 - instruments.size.toLong()
//        )
//    )
//    instruments.add(
//        Instrument(
//            name = "Test instrument 2",
//            nameResource = null,
//            strings = intArrayOf(0, 1, 1),
//            iconResource = R.drawable.ic_violin,
//            stableId = -1 - instruments.size.toLong()
//        )
//    )
    return instruments
}

package de.moekadu.tuner

import android.content.Context

enum class InstrumentType {
    Piano, Guitar, Bass, Ukulele
}

data class Instrument(private val name: CharSequence?, private val nameResource: Int?, val strings: IntArray, val type: InstrumentType,
                      val iconResource: Int, val stableId: Long) {
    val stringsSorted = strings.map { it.toFloat() }.toFloatArray().sortedArray()

    fun getNameString(context: Context): CharSequence {
        return when {
            nameResource != null -> context.getString(nameResource)
            name != null -> name
            else -> throw RuntimeException("No name given for instrument")
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
        if (type != other.type) return false
        if (iconResource != other.iconResource) return false
        if (stableId != other.stableId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + strings.contentHashCode()
        result = 31 * result + type.hashCode()
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
            type = InstrumentType.Piano,
            iconResource = R.drawable.ic_piano,
            stableId = instruments.size.toLong() // this should be set by a id generator
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.guitar_eadgbe,
            strings = intArrayOf(-29, -24, -19, -14, -10, -5),
            type = InstrumentType.Guitar,
            iconResource = R.drawable.ic_guitar,
            stableId = instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.bass_eadg,
            strings = intArrayOf(-41, -36, -31, -26),
            type = InstrumentType.Bass,
            iconResource = R.drawable.ic_bass,
            stableId = instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.bass_beadg,
            strings = intArrayOf(-46, -41, -36, -31, -26),
            type = InstrumentType.Bass,
            iconResource = R.drawable.ic_bass,
            stableId = instruments.size.toLong()
        )
    )
    instruments.add(
        Instrument(
            name = null,
            nameResource = R.string.ukulele_gcea,
            strings = intArrayOf(-2, -9, -5, 0),
            type = InstrumentType.Ukulele,
            iconResource = R.drawable.ic_ukulele,
            stableId = instruments.size.toLong()
        )
    )
    return instruments
}

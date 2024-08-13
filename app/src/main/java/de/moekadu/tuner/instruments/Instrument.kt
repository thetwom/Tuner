/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.instruments

import android.content.Context
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.asAnnotatedString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Immutable
data class Instrument(private val name: String?, private val nameResource: Int?, val strings: Array<MusicalNote>,
                      val icon: InstrumentIcon, val stableId: Long, val isChromatic: Boolean = false) :
    Parcelable {

    /** Get instrument name as string.
     * @param context Context is only needed, if the instrument name is a string resource.
     * @return Name of instrument.
     */
    fun getNameString(context: Context?): String {
        return when {
            nameResource != null && context != null -> context.getString(nameResource)
            name != null -> name.toString()
            else -> throw RuntimeException("No name given for instrument")
        }
    }

    /** Get readable representation of all strings (e.g. "A#4 - C5 - G5")
     * @param context Context for obtaining string resources.
     * @param notePrintOptions How to print notes.
     */
    fun getStringsString(
        context: Context,
        notePrintOptions: NotePrintOptions,
        fontSize: TextUnit,
        fontWeight: FontWeight? = null
        ): AnnotatedString {
        return if (isChromatic) {
            buildAnnotatedString { append(context.getString(R.string.chromatic)) }
        } else {
            buildAnnotatedString {
//            Log.v("Tuner", "Instrument.getStringsString: printOption=$printOption, preferFlat=$preferFlat")
                if (strings.isNotEmpty())
                    append(strings[0].asAnnotatedString(
                        notePrintOptions,
                        fontSize,
                        fontWeight,
                        withOctave = true,
                        resources = context.resources
                    ))

                for (i in 1 until strings.size) {
                    append(" - ")
                    append(strings[i].asAnnotatedString(
                        notePrintOptions,
                        fontSize,
                        fontWeight,
                        withOctave = true,
                        resources = context.resources
                    ))
                }
            }
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
        if (icon != other.icon) return false
        if (stableId != other.stableId) return false
        if (isChromatic != other.isChromatic) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (nameResource ?: 0)
        result = 31 * result + strings.contentHashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + stableId.hashCode()
        result = 31 * result + isChromatic.hashCode()
        return result
    }
}

val instrumentChromatic = Instrument(
    name = null,
    nameResource = R.string.chromatic,
    strings = arrayOf(),
    icon = InstrumentIcon.piano,
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
            icon = InstrumentIcon.guitar,
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
            icon = InstrumentIcon.bass,
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
            icon = InstrumentIcon.bass,
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
            icon = InstrumentIcon.ukulele,
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
            icon = InstrumentIcon.violin,
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
            icon = InstrumentIcon.violin,
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
            icon = InstrumentIcon.cello,
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
            icon = InstrumentIcon.double_bass,
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

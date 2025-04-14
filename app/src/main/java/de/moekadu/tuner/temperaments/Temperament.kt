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
package de.moekadu.tuner.temperaments

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.misc.GetTextFromResId
import de.moekadu.tuner.misc.GetTextFromResIdWithIntArg
import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.misc.StringOrResId
import kotlinx.serialization.Serializable
import kotlin.math.log
import kotlin.math.pow

fun ratioToCents(ratio: Float): Float {
    return (1200.0 * log(ratio.toDouble(), 2.0)).toFloat()
}

fun ratioToCents(ratio: Double): Double {
    return (1200.0 * log(ratio, 2.0))
}

fun centsToFrequency(cent: Double, referenceFrequency: Double): Double {
    return referenceFrequency * centsToRatio(cent)
}

fun frequencyToCents(frequency: Double, referenceFrequency: Double): Double {
    return ratioToCents(frequency / referenceFrequency)
}

private fun centsToRatio(cents: Float): Float {
    return (2.0.pow(cents / 1200.0)).toFloat()
}

private fun centsToRatio(cents: Double): Double {
    return (2.0.pow(cents / 1200.0))
}

/** Old temperament class. */
@Serializable
@Immutable
data class Temperament(
    val name: StringOrResId,
    val abbreviation: StringOrResId,
    val description: StringOrResId,
    val cents: DoubleArray,
    val rationalNumbers: Array<RationalNumber>?,
    val circleOfFifths: TemperamentCircleOfFifths?,
    val equalOctaveDivision: Int?,
    val stableId: Long
) {
    fun toNew(): Temperament2 {
        return Temperament2(
            name = name.toGetText(),
            abbreviation = abbreviation.toGetText(),
            description = description.toGetText(),
            cents = cents,
            rationalNumbers = rationalNumbers,
            circleOfFifths = circleOfFifths,
            equalOctaveDivision = equalOctaveDivision,
            stableId = stableId
        )
    }
}

/** Temperament class which divides octaves into single notes.
 * @param name Name of temperament.
 * @param abbreviation Short name for temperament.
 * @param description Description of temperament.
 *  @param cents Ratio of a note frequency to the octave root note frequency, given as cents.This
 *    includes the octave value, which must be 1200.
 *      Cents are defined as 1200 * log2(noteFrequency / rootFrequency).
 *  @param rationalNumbers For each note, the rational number ratio as
 *       noteFrequency / rootNoteFrequency
 *    If the ratios are no rational numbers or the ratio as rational number is of no importance
 *    to the temperament, this can be null.
 *  @param circleOfFifths If the temperament can be described as a circle of fifths, give
 *    a none-null value here.
 *  @param equalOctaveDivision If the temperament a equally divided octave, define number of
 *    notes per octave here. So for the classic edo scale (c, c#, d, eb, e, f, f#, g, ab, a, b)
 *    this would be 12. Use null if the temperament is not an equally divided octave scale.
 *  @param stableId Unique id to identify the temperament.
 */
@Serializable
@Immutable
data class Temperament2(
    val name: GetText,
    val abbreviation: GetText,
    val description: GetText,
    val cents: DoubleArray,
    val rationalNumbers: Array<RationalNumber>?,
    val circleOfFifths: TemperamentCircleOfFifths?,
    val equalOctaveDivision: Int?,
    val stableId: Long
) {
    /** Number of notes per octave. */
    val numberOfNotesPerOctave get() = cents.size - 1

    /** Create temperament if cents are known.
     * @param name Name of temperament.
     * @param abbreviation Short name for temperament.
     * @param description Temperament description.
     * @param cents Cents, including 1200 as the octave value.
     * @param stableId Unique id to identify the temperament.
     */
    constructor(
        name: GetText,
        abbreviation: GetText,
        description: GetText,
        cents: DoubleArray,
        stableId: Long
    ): this(
        name, abbreviation, description, cents,
        null, null, null, stableId
    )


    /** Create temperament if rational numbers are known.
     * @param name Name of temperament.
     * @param abbreviation Short name for temperament.
     * @param description Temperament description.
     * @param rationalNumbers Rational numbers including 2 for the octave value.
     * @param stableId Unique id to identify the temperament.
     */
    constructor(
        name: GetText,
        abbreviation: GetText,
        description: GetText,
        rationalNumbers: Array<RationalNumber>,
        stableId: Long
    ) : this(
        name, abbreviation, description,
        rationalNumbers.map { ratioToCents(it.toDouble()) }.toDoubleArray(),
        rationalNumbers,null, null, stableId
    )

    /** Create temperament if circle of fifths definition is known.
     * @param name Name of temperament.
     * @param abbreviation Short name for temperament.
     * @param description Temperament description.
     * @param circleOfFifths Circle of fifths.
     * @param stableId Unique id to identify the temperament.
     */
    constructor(
        name: GetText,
        abbreviation: GetText,
        description: GetText,
        circleOfFifths: TemperamentCircleOfFifths,
        stableId: Long
    ) : this(
        name, abbreviation, description,
        circleOfFifths.getRatios().map { ratioToCents(it) }.toDoubleArray(),
        null, circleOfFifths, null, stableId
    )

    /** Create equally divided octave temperament.
     * @param name Name of temperament.
     * @param abbreviation Short name for temperament.
     * @param description Temperament description.
     * @param equalOctaveDivision Number of notes within octave (So the classical scale with
     *    12 notes per octave would require the number 12)
     * @param stableId Unique id to identify the temperament.
     */
    constructor(
        name: GetText,
        abbreviation: GetText,
        description: GetText,
        equalOctaveDivision: Int,
        stableId: Long
    ) : this(
        name, abbreviation, description,
        DoubleArray(equalOctaveDivision + 1) {
            it * 1200.0 / equalOctaveDivision.toDouble()
        },
        null,
        if (equalOctaveDivision == 12)
            circleOfFifthsEDO12
        else
            null,
        equalOctaveDivision,
        stableId
    )

    companion object {
        const val NO_STABLE_ID = Long.MAX_VALUE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Temperament2

        if (name != other.name) return false
        if (abbreviation != other.abbreviation) return false
        if (description != other.description) return false
        if (!cents.contentEquals(other.cents)) return false
        if (rationalNumbers != null) {
            if (other.rationalNumbers == null) return false
            if (!rationalNumbers.contentEquals(other.rationalNumbers)) return false
        } else if (other.rationalNumbers != null) return false
        if (circleOfFifths != other.circleOfFifths) return false
        if (equalOctaveDivision != other.equalOctaveDivision) return false
        if (stableId != other.stableId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + abbreviation.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + cents.contentHashCode()
        result = 31 * result + (rationalNumbers?.contentHashCode() ?: 0)
        result = 31 * result + (circleOfFifths?.hashCode() ?: 0)
        result = 31 * result + (equalOctaveDivision ?: 0)
        result = 31 * result + stableId.hashCode()
        return result
    }
}

val temperamentDatabase = createPredefinedTemperaments()

/** Create test temperament */
fun createTestTemperamentEdo12(): Temperament2 {
    return Temperament2(
        GetTextFromResIdWithIntArg(R.string.equal_temperament_x, 12),
        GetTextFromResIdWithIntArg(R.string.equal_temperament_x_abbr, 12),
        GetTextFromResIdWithIntArg(R.string.equal_temperament_x_desc, 12),
        12,
        1L
    )
}

/** Create test temperament */
fun createTestTemperamentWerckmeisterVI(): Temperament2 {
    return Temperament2(
        GetTextFromResId(R.string.werckmeister_vi),
        GetTextFromResId(R.string.werckmeister_vi_abbr),
        GetTextFromResId(R.string.werckmeister_vi_desc),
        rationalNumberTemperamentWerckmeisterVI,
        1L
    )
}

private fun createPredefinedTemperaments(): ArrayList<Temperament2> {
    val temperaments = ArrayList<Temperament2>()

    // edo12
    temperaments.add(
        Temperament2(
            GetTextFromResIdWithIntArg(R.string.equal_temperament_x, 12),
            GetTextFromResIdWithIntArg(R.string.equal_temperament_x_abbr, 12),
            GetTextFromResIdWithIntArg(R.string.equal_temperament_x_desc, 12),
            12,
            (-1 - temperaments.size).toLong()
        )
    )

//    // edo17
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_17),
//            GetTextFromResId(R.string.equal_temperament_17_abbr),
//            GetTextFromResId(R.string.equal_temperament_17_desc),
//            17,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo19
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_19),
//            GetTextFromResId(R.string.equal_temperament_19_abbr),
//            GetTextFromResId(R.string.equal_temperament_19_desc),
//            19,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo22
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_22),
//            GetTextFromResId(R.string.equal_temperament_22_abbr),
//            GetTextFromResId(R.string.equal_temperament_22_desc),
//            22,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo24
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_24),
//            GetTextFromResId(R.string.equal_temperament_24_abbr),
//            GetTextFromResId(R.string.equal_temperament_24_desc),
//            24,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo27
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_27),
//            GetTextFromResId(R.string.equal_temperament_27_abbr),
//            GetTextFromResId(R.string.equal_temperament_27_desc),
//            27,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo29
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_29),
//            GetTextFromResId(R.string.equal_temperament_29_abbr),
//            GetTextFromResId(R.string.equal_temperament_29_desc),
//            29,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo31
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_31),
//            GetTextFromResId(R.string.equal_temperament_31_abbr),
//            GetTextFromResId(R.string.equal_temperament_31_desc),
//            31,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo41
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_41),
//            GetTextFromResId(R.string.equal_temperament_41_abbr),
//            GetTextFromResId(R.string.equal_temperament_41_desc),
//            41,
//            (-1 - temperaments.size).toLong()
//        )
//    )
//
//    // edo53
//    temperaments.add(
//        Temperament2(
//            GetTextFromResId(R.string.equal_temperament_53),
//            GetTextFromResId(R.string.equal_temperament_53_abbr),
//            GetTextFromResId(R.string.equal_temperament_53_desc),
//            53,
//            (-1 - temperaments.size).toLong()
//        )
//    )

    // pythagorean
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.pythagorean_tuning),
            GetTextFromResId(R.string.pythagorean_tuning_abbr),
            GetTextFromString(""),
            circleOfFifthsPythagorean,
            (-1 - temperaments.size).toLong()
        )
    )

    // pure
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.pure_tuning),
            GetTextFromResId(R.string.pure_tuning_abbr),
            GetTextFromResId(R.string.pure_tuning_desc),
            rationalNumberTemperamentPure,
            (-1 - temperaments.size).toLong()
        )
    )

    // quarter-comma mean
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.quarter_comma_mean_tone),
            GetTextFromResId(R.string.quarter_comma_mean_tone_abbr),
            GetTextFromResId(R.string.quarter_comma_mean_tone_desc),
            circleOfFifthsQuarterCommaMeanTone,
            (-1 - temperaments.size).toLong()
        )
    )

    // extended quarter-comma mean
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.extended_quarter_comma_mean_tone),
            GetTextFromResId(R.string.extended_quarter_comma_mean_tone_abbr),
            GetTextFromResId(R.string.extended_quarter_comma_mean_tone_desc),
            extendedQuarterCommaMeantone,
            (-1 - temperaments.size).toLong()
        )
    )

    // third-comma mean
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.third_comma_mean_tone),
            GetTextFromResId(R.string.third_comma_mean_tone_abbr),
            GetTextFromResId(R.string.third_comma_mean_tone_desc),
            circleOfFifthsThirdCommaMeanTone,
            (-1 - temperaments.size).toLong()
        )
    )

    // fifth-comma mean
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.fifth_comma_mean_tone),
            GetTextFromResId(R.string.fifth_comma_mean_tone_abbr),
            GetTextFromResId(R.string.fifth_comma_mean_tone_desc),
            circleOfFifthsFifthCommaMeanTone,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister III
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.werckmeister_iii),
            GetTextFromResId(R.string.werckmeister_iii_abbr),
            GetTextFromResId(R.string.werckmeister_iii_desc),
            circleOfFifthsWerckmeisterIII,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister IV
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.werckmeister_iv),
            GetTextFromResId(R.string.werckmeister_iv_abbr),
            GetTextFromResId(R.string.werckmeister_iv_desc),
            circleOfFifthsWerckmeisterIV,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister V
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.werckmeister_v),
            GetTextFromResId(R.string.werckmeister_v_abbr),
            GetTextFromResId(R.string.werckmeister_v_desc),
            circleOfFifthsWerckmeisterV,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister VI
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.werckmeister_vi),
            GetTextFromResId(R.string.werckmeister_vi_abbr),
            GetTextFromResId(R.string.werckmeister_vi_desc),
            rationalNumberTemperamentWerckmeisterVI,
            (-1 - temperaments.size).toLong()
        )
    )

    // Kirnberger 1
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.kirnberger1),
            GetTextFromResId(R.string.kirnberger1_abbr),
            GetTextFromResId(R.string.kirnberger1_desc),
            circleOfFifthsKirnberger1,
            (-1 - temperaments.size).toLong()
        )
    )

    // Kirnberger 2
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.kirnberger2),
            GetTextFromResId(R.string.kirnberger2_abbr),
            GetTextFromResId(R.string.kirnberger2_desc),
            circleOfFifthsKirnberger2,
            (-1 - temperaments.size).toLong()
        )
    )

    // Kirnberger 3
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.kirnberger3),
            GetTextFromResId(R.string.kirnberger3_abbr),
            GetTextFromResId(R.string.kirnberger3_desc),
            circleOfFifthsKirnberger3,
            (-1 - temperaments.size).toLong()
        )
    )

    // Neidhardt 1
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.neidhardt1),
            GetTextFromResId(R.string.neidhardt1_abbr),
            GetTextFromResId(R.string.neidhardt1_desc),
            circleOfFifthsNeidhardt1,
            (-1 - temperaments.size).toLong()
        )
    )

    // Neidhardt 2
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.neidhardt2),
            GetTextFromResId(R.string.neidhardt2_abbr),
            GetTextFromResId(R.string.neidhardt2_desc),
            circleOfFifthsNeidhardt2,
            (-1 - temperaments.size).toLong()
        )
    )

    // Neidhardt 3
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.neidhardt3),
            GetTextFromResId(R.string.neidhardt3_abbr),
            GetTextFromResId(R.string.neidhardt3_desc),
            circleOfFifthsNeidhardt3,
            (-1 - temperaments.size).toLong()
        )
    )

    // Valotti
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.valotti),
            GetTextFromResId(R.string.valotti_abbr),
            GetTextFromString(""),
            circleOfFifthsValotti,
            (-1 - temperaments.size).toLong()
        )
    )

    // Young 2
    temperaments.add(
        Temperament2(
            GetTextFromResId(R.string.young2),
            GetTextFromResId(R.string.young2_abbr),
            GetTextFromString(""),
            circleOfFifthsYoung2,
            (-1 - temperaments.size).toLong()
        )
    )

    return temperaments
}
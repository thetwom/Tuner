package de.moekadu.tuner.temperaments2

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.TemperamentCircleOfFifths
import de.moekadu.tuner.temperaments.circleOfFifthsEDO12
import de.moekadu.tuner.temperaments.circleOfFifthsFifthCommaMeanTone
import de.moekadu.tuner.temperaments.circleOfFifthsKirnberger1
import de.moekadu.tuner.temperaments.circleOfFifthsKirnberger2
import de.moekadu.tuner.temperaments.circleOfFifthsKirnberger3
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt1
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt2
import de.moekadu.tuner.temperaments.circleOfFifthsNeidhardt3
import de.moekadu.tuner.temperaments.circleOfFifthsPythagorean
import de.moekadu.tuner.temperaments.circleOfFifthsQuarterCommaMeanTone
import de.moekadu.tuner.temperaments.circleOfFifthsThirdCommaMeanTone
import de.moekadu.tuner.temperaments.circleOfFifthsValotti
import de.moekadu.tuner.temperaments.circleOfFifthsWerckmeisterIII
import de.moekadu.tuner.temperaments.circleOfFifthsWerckmeisterIV
import de.moekadu.tuner.temperaments.circleOfFifthsWerckmeisterV
import de.moekadu.tuner.temperaments.circleOfFifthsYoung2
import de.moekadu.tuner.temperaments.extendedQuarterCommaMeantone
import de.moekadu.tuner.temperaments.rationalNumberTemperamentWerckmeisterVI
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
    /** Number of notes per octave. */
    val numberOfNotesPerOctave get() = cents.size - 1

    companion object {
        /** Create temperament if cents are known.
         * @param name Name of temperament.
         * @param abbreviation Short name for temperament.
         * @param description Temperament description.
         * @param cents Cents, including 1200 as the octave value.
         * @param stableId Unique id to identify the temperament.
         * @return Temperament.
         */
        fun create(
            name: StringOrResId,
            abbreviation: StringOrResId,
            description: StringOrResId,
            cents: DoubleArray,
            stableId: Long
        )
                : Temperament {
            return Temperament(
                name,
                abbreviation,
                description,
                cents,
                null,
                null,
                null,
                stableId
            )
        }

        /** Create temperament if rational numbers are known.
         * @param name Name of temperament.
         * @param abbreviation Short name for temperament.
         * @param description Temperament description.
         * @param rationalNumbers Rational numbers including 2 for the octave value.
         * @param stableId Unique id to identify the temperament.
         * @return Temperament.
         */
        fun create(
            name: StringOrResId,
            abbreviation: StringOrResId,
            description: StringOrResId,
            rationalNumbers: Array<RationalNumber>,
            stableId: Long
        )
                : Temperament {
            return Temperament(
                name,
                abbreviation,
                description,
                rationalNumbers.map { ratioToCents(it.toDouble()) }.toDoubleArray(),
                rationalNumbers,
                null,
                null,
                stableId
            )
        }

        /** Create temperament if circle of fifths definition is known.
         * @param name Name of temperament.
         * @param abbreviation Short name for temperament.
         * @param description Temperament description.
         * @param circleOfFifths Circle of fifths.
         * @param stableId Unique id to identify the temperament.
         * @return Temperament.
         */
        fun create(
            name: StringOrResId,
            abbreviation: StringOrResId,
            description: StringOrResId,
            circleOfFifths: TemperamentCircleOfFifths,
            stableId: Long
        ): Temperament {
            return Temperament(
                name,
                abbreviation,
                description,
                circleOfFifths.getRatios().map { ratioToCents(it) }.toDoubleArray(),
                null,
                circleOfFifths,
                null,
                stableId
            )
        }

        /** Create equally divided octave temperament.
         * @param name Name of temperament.
         * @param abbreviation Short name for temperament.
         * @param description Temperament description.
         * @param equalOctaveDivision Number of notes within octave (So the classical scale with
         *    12 notes per octave would require the number 12)
         * @param stableId Unique id to identify the temperament.
         * @return Temperament.
         */
        fun create(
            name: StringOrResId,
            abbreviation: StringOrResId,
            description: StringOrResId,
            equalOctaveDivision: Int,
            stableId: Long
        ): Temperament {
            val circleOfFifths = if (equalOctaveDivision == 12)
                circleOfFifthsEDO12
            else
                null
            val cents = DoubleArray(equalOctaveDivision + 1) {
                it * 1200.0 / equalOctaveDivision.toDouble()
            }
            return Temperament(
                name,
                abbreviation,
                description,
                cents,
                null,
                circleOfFifths,
                equalOctaveDivision,
                stableId
            )
        }

        const val NO_STABLE_ID = Long.MAX_VALUE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Temperament

//        if (name != other.name) return false
//        if (abbreviation != other.abbreviation) return false
//        if (description != other.description) return false
//        if (!cents.contentEquals(other.cents)) return false
//        if (rationalNumbers != null) {
//            if (other.rationalNumbers == null) return false
//            if (!rationalNumbers.contentEquals(other.rationalNumbers)) return false
//        } else if (other.rationalNumbers != null) return false
//        if (circleOfFifths != other.circleOfFifths) return false
//        if (equalOctaveDivision != other.equalOctaveDivision) return false
        if (stableId != other.stableId) return false

        return true
    }

    override fun hashCode(): Int {
//        var result = name.hashCode()
//        result = 31 * result + abbreviation.hashCode()
//        result = 31 * result + description.hashCode()
//        result = 31 * result + cents.contentHashCode()
//        result = 31 * result + (rationalNumbers?.contentHashCode() ?: 0)
//        result = 31 * result + (circleOfFifths?.hashCode() ?: 0)
//        result = 31 * result + (equalOctaveDivision ?: 0)
//        result = 31 * result + stableId.hashCode()
//        return result
        return stableId.hashCode()
    }
}

val temperamentDatabase = createPredefinedTemperaments()

private fun createPredefinedTemperaments(): ArrayList<Temperament> {
    val temperaments = ArrayList<Temperament>()

    // edo12
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_12),
            StringOrResId(R.string.equal_temperament_12_abbr),
            StringOrResId(R.string.equal_temperament_12_desc),
            12,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo17
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_17),
            StringOrResId(R.string.equal_temperament_17_abbr),
            StringOrResId(R.string.equal_temperament_17_desc),
            17,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo19
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_19),
            StringOrResId(R.string.equal_temperament_19_abbr),
            StringOrResId(R.string.equal_temperament_19_desc),
            19,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo22
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_22),
            StringOrResId(R.string.equal_temperament_22_abbr),
            StringOrResId(R.string.equal_temperament_22_desc),
            22,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo24
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_24),
            StringOrResId(R.string.equal_temperament_24_abbr),
            StringOrResId(R.string.equal_temperament_24_desc),
            24,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo27
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_27),
            StringOrResId(R.string.equal_temperament_27_abbr),
            StringOrResId(R.string.equal_temperament_27_desc),
            27,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo29
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_29),
            StringOrResId(R.string.equal_temperament_29_abbr),
            StringOrResId(R.string.equal_temperament_29_desc),
            29,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo31
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_31),
            StringOrResId(R.string.equal_temperament_31_abbr),
            StringOrResId(R.string.equal_temperament_31_desc),
            31,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo41
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_41),
            StringOrResId(R.string.equal_temperament_41_abbr),
            StringOrResId(R.string.equal_temperament_41_desc),
            41,
            (-1 - temperaments.size).toLong()
        )
    )

    // edo53
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.equal_temperament_53),
            StringOrResId(R.string.equal_temperament_53_abbr),
            StringOrResId(R.string.equal_temperament_53_desc),
            53,
            (-1 - temperaments.size).toLong()
        )
    )

    // pythagorean
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.pythagorean_tuning),
            StringOrResId(R.string.pythagorean_tuning_abbr),
            StringOrResId(""),
            circleOfFifthsPythagorean,
            (-1 - temperaments.size).toLong()
        )
    )

    // pure
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.pure_tuning),
            StringOrResId(R.string.pure_tuning_abbr),
            StringOrResId(R.string.pure_tuning_desc),
            de.moekadu.tuner.temperaments.rationalNumberTemperamentPure,
            (-1 - temperaments.size).toLong()
        )
    )

    // quarter-comma mean
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.quarter_comma_mean_tone),
            StringOrResId(R.string.quarter_comma_mean_tone_abbr),
            StringOrResId(R.string.quarter_comma_mean_tone_desc),
            circleOfFifthsQuarterCommaMeanTone,
            (-1 - temperaments.size).toLong()
        )
    )

    // extended quarter-comma mean
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.extended_quarter_comma_mean_tone),
            StringOrResId(R.string.extended_quarter_comma_mean_tone_abbr),
            StringOrResId(R.string.extended_quarter_comma_mean_tone_desc),
            extendedQuarterCommaMeantone,
            (-1 - temperaments.size).toLong()
        )
    )

    // third-comma mean
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.third_comma_mean_tone),
            StringOrResId(R.string.third_comma_mean_tone_abbr),
            StringOrResId(R.string.third_comma_mean_tone_desc),
            circleOfFifthsThirdCommaMeanTone,
            (-1 - temperaments.size).toLong()
        )
    )

    // fifth-comma mean
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.fifth_comma_mean_tone),
            StringOrResId(R.string.fifth_comma_mean_tone_abbr),
            StringOrResId(R.string.fifth_comma_mean_tone_desc),
            circleOfFifthsFifthCommaMeanTone,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister III
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.werckmeister_iii),
            StringOrResId(R.string.werckmeister_iii_abbr),
            StringOrResId(R.string.werckmeister_iii_desc),
            circleOfFifthsWerckmeisterIII,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister IV
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.werckmeister_iv),
            StringOrResId(R.string.werckmeister_iv_abbr),
            StringOrResId(R.string.werckmeister_iv_desc),
            circleOfFifthsWerckmeisterIV,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister V
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.werckmeister_v),
            StringOrResId(R.string.werckmeister_v_abbr),
            StringOrResId(R.string.werckmeister_v_desc),
            circleOfFifthsWerckmeisterV,
            (-1 - temperaments.size).toLong()
        )
    )

    // Werckmeister VI
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.werckmeister_vi),
            StringOrResId(R.string.werckmeister_vi_abbr),
            StringOrResId(R.string.werckmeister_vi_desc),
            rationalNumberTemperamentWerckmeisterVI,
            (-1 - temperaments.size).toLong()
        )
    )

    // Kirnberger 1
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.kirnberger1),
            StringOrResId(R.string.kirnberger1_abbr),
            StringOrResId(R.string.kirnberger1_desc),
            circleOfFifthsKirnberger1,
            (-1 - temperaments.size).toLong()
        )
    )

    // Kirnberger 2
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.kirnberger2),
            StringOrResId(R.string.kirnberger2_abbr),
            StringOrResId(R.string.kirnberger2_desc),
            circleOfFifthsKirnberger2,
            (-1 - temperaments.size).toLong()
        )
    )

    // Kirnberger 3
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.kirnberger3),
            StringOrResId(R.string.kirnberger3_abbr),
            StringOrResId(R.string.kirnberger3_desc),
            circleOfFifthsKirnberger3,
            (-1 - temperaments.size).toLong()
        )
    )

    // Neidhardt 1
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.neidhardt1),
            StringOrResId(R.string.neidhardt1_abbr),
            StringOrResId(R.string.neidhardt1_desc),
            circleOfFifthsNeidhardt1,
            (-1 - temperaments.size).toLong()
        )
    )

    // Neidhardt 2
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.neidhardt2),
            StringOrResId(R.string.neidhardt2_abbr),
            StringOrResId(R.string.neidhardt2_desc),
            circleOfFifthsNeidhardt2,
            (-1 - temperaments.size).toLong()
        )
    )

    // Neidhardt 3
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.neidhardt3),
            StringOrResId(R.string.neidhardt3_abbr),
            StringOrResId(R.string.neidhardt3_desc),
            circleOfFifthsNeidhardt3,
            (-1 - temperaments.size).toLong()
        )
    )

    // Valotti
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.valotti),
            StringOrResId(R.string.valotti_abbr),
            StringOrResId(""),
            circleOfFifthsValotti,
            (-1 - temperaments.size).toLong()
        )
    )

    // Young 2
    temperaments.add(
        Temperament.create(
            StringOrResId(R.string.young2),
            StringOrResId(R.string.young2_abbr),
            StringOrResId(""),
            circleOfFifthsYoung2,
            (-1 - temperaments.size).toLong()
        )
    )

    return temperaments
}
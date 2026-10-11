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
package de.moekadu.tuner.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.moekadu.tuner.R
import de.moekadu.tuner.notedetection.TuningState
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.theme.tunerColors
import kotlin.math.abs
import kotlin.math.min

// Positions on the staff are counted in diatonic steps: C0 = 0, D0 = 1, ..., C4 = 28.
// One diatonic step is half a staff space.
private const val TREBLE_BOTTOM_LINE = 30 // E4
private const val BASS_BOTTOM_LINE = 18 // G2
private const val STAFF_HEIGHT = 8 // positions from bottom to top line
private const val MIDDLE_C = 28

/** Number of ledger lines drawn above/below the staff; notes beyond are drawn octaves shifted
 * and the clef gets an octave mark (8, 15, ...).
 */
private const val MAX_LEDGER_LINES = 3

/** Clef setup of the note staff. */
enum class StaffClef {
    /** Treble and bass staff, as for piano. */
    Grand,
    Treble,

    /** Treble clef with an 8 below, notes are written one octave above the sung pitch. */
    Tenor,
    Bass
}

private enum class ClefGlyph { Treble, Bass }

/** Five-line staff with a clef.
 * @param bottomLine Staff position of the lowest line.
 */
private data class SingleStaff(val glyph: ClefGlyph, val bottomLine: Int) {
    val topLine get() = bottomLine + STAFF_HEIGHT

    /** Line which the clef refers to: G line for treble, F line for bass. */
    val clefLine get() = if (glyph == ClefGlyph.Treble) bottomLine + 2 else bottomLine + 6

    /** Offset of key signature positions relative to the treble staff positions. */
    val keySignatureOffset get() = bottomLine - TREBLE_BOTTOM_LINE
}

/** Staves of a staff setup.
 * @param staves Staves from top to bottom.
 * @param writtenOctaves Octaves by which the notes are written above their sounding pitch.
 */
private class StaffLayout(val staves: List<SingleStaff>, val writtenOctaves: Int) {
    val highestPosition = staves.first().topLine + 2 * MAX_LEDGER_LINES
    val lowestPosition = staves.last().bottomLine - 2 * MAX_LEDGER_LINES
    val centerPosition = (highestPosition + lowestPosition) / 2
    val staffSpacesVisible = (highestPosition - lowestPosition) / 2 + 2
}

private val trebleStaff = SingleStaff(ClefGlyph.Treble, TREBLE_BOTTOM_LINE)
private val bassStaff = SingleStaff(ClefGlyph.Bass, BASS_BOTTOM_LINE)

private fun StaffClef.layout() = when (this) {
    StaffClef.Grand -> StaffLayout(listOf(trebleStaff, bassStaff), 0)
    StaffClef.Treble -> StaffLayout(listOf(trebleStaff), 0)
    StaffClef.Tenor -> StaffLayout(listOf(trebleStaff), 1)
    StaffClef.Bass -> StaffLayout(listOf(bassStaff), 0)
}

// Gonville clef outlines, traced with gonville/clef_paths.py. Units are staff spaces,
// x starts at the left edge of the glyph, y = 0 is the clef line and y points downwards.
private const val TREBLE_CLEF_WIDTH = 2.642f
private const val TREBLE_CLEF_TOP = -4.571f
private const val TREBLE_CLEF_BOTTOM = 2.673f
private const val BASS_CLEF_WIDTH = 2.791f
private const val BASS_CLEF_TOP = -1.077f
private const val BASS_CLEF_BOTTOM = 2.3f

// Generated path data, kept on single lines so it can be replaced as a whole.
@Suppress("ktlint:standard:max-line-length")
private const val TREBLE_CLEF_PATH = "M1.529 -4.567C1.512 -4.564 1.501 -4.557 1.468 -4.528C1.275 -4.363 1.12 -4.14 1.027 -3.896C0.966 -3.735 0.931 -3.574 0.92 -3.392C0.916 -3.34 0.918 -3.198 0.923 -3.149C0.934 -3.035 0.939 -3.004 1.004 -2.703C1.011 -2.673 1.034 -2.563 1.056 -2.459L1.097 -2.27L1.003 -2.176C0.952 -2.124 0.894 -2.064 0.874 -2.043C0.662 -1.818 0.475 -1.592 0.3 -1.348C0.238 -1.262 0.194 -1.189 0.154 -1.106C0.072 -0.935 0.025 -0.769 0.005 -0.574C0 -0.52 0 -0.355 0.005 -0.304C0.019 -0.175 0.038 -0.087 0.075 0.026C0.132 0.202 0.222 0.364 0.342 0.508C0.496 0.692 0.693 0.835 0.915 0.924C1.042 0.975 1.169 1.007 1.317 1.023C1.362 1.028 1.529 1.028 1.572 1.024C1.638 1.017 1.698 1.007 1.756 0.993C1.775 0.988 1.792 0.984 1.792 0.984C1.793 0.985 1.878 1.382 1.889 1.435C1.922 1.601 1.931 1.754 1.914 1.884C1.9 1.997 1.867 2.1 1.82 2.176C1.789 2.225 1.761 2.262 1.72 2.304C1.658 2.367 1.599 2.41 1.523 2.447C1.456 2.48 1.397 2.5 1.325 2.513C1.279 2.521 1.247 2.524 1.193 2.524C1.128 2.524 1.081 2.518 1.019 2.503C0.974 2.492 0.908 2.469 0.874 2.452L0.864 2.447L0.895 2.446C0.931 2.444 0.967 2.438 0.999 2.427C1.031 2.417 1.084 2.391 1.11 2.372C1.134 2.355 1.171 2.321 1.189 2.298C1.243 2.233 1.275 2.152 1.281 2.065C1.285 2.004 1.275 1.94 1.254 1.884C1.195 1.734 1.061 1.633 0.898 1.614C0.869 1.611 0.808 1.613 0.78 1.618C0.709 1.63 0.643 1.659 0.586 1.702C0.543 1.733 0.494 1.789 0.465 1.837C0.382 1.978 0.382 2.153 0.464 2.288C0.512 2.367 0.625 2.473 0.726 2.534C0.842 2.605 0.966 2.647 1.101 2.662C1.2 2.673 1.306 2.666 1.404 2.642C1.616 2.59 1.798 2.464 1.919 2.285C1.98 2.196 2.02 2.097 2.044 1.976C2.06 1.892 2.066 1.824 2.064 1.72C2.062 1.575 2.054 1.525 1.98 1.18C1.952 1.048 1.929 0.939 1.929 0.938C1.929 0.937 1.94 0.932 1.954 0.926C1.985 0.913 2.054 0.877 2.086 0.858C2.326 0.717 2.507 0.492 2.585 0.24C2.605 0.176 2.615 0.134 2.624 0.072C2.642 -0.059 2.634 -0.195 2.601 -0.324C2.582 -0.398 2.557 -0.465 2.528 -0.522C2.47 -0.639 2.419 -0.711 2.331 -0.799C2.248 -0.882 2.167 -0.94 2.064 -0.991C1.96 -1.042 1.858 -1.071 1.732 -1.087C1.697 -1.092 1.555 -1.091 1.522 -1.086C1.508 -1.084 1.497 -1.083 1.496 -1.083C1.496 -1.083 1.461 -1.248 1.418 -1.45L1.339 -1.817L1.379 -1.855C1.4 -1.876 1.447 -1.921 1.483 -1.955C1.554 -2.022 1.582 -2.05 1.629 -2.102C1.75 -2.234 1.852 -2.383 1.928 -2.537C2.031 -2.747 2.09 -2.965 2.104 -3.192C2.108 -3.244 2.106 -3.38 2.101 -3.428C2.086 -3.581 2.058 -3.706 2.004 -3.868C1.928 -4.097 1.824 -4.293 1.699 -4.443C1.673 -4.475 1.618 -4.529 1.595 -4.546C1.569 -4.565 1.551 -4.571 1.529 -4.567ZM1.735 -3.956C1.775 -3.945 1.81 -3.924 1.833 -3.896C1.853 -3.873 1.864 -3.846 1.875 -3.796C1.897 -3.699 1.896 -3.593 1.874 -3.471C1.866 -3.428 1.849 -3.359 1.837 -3.32C1.798 -3.2 1.735 -3.062 1.663 -2.942C1.574 -2.793 1.475 -2.66 1.35 -2.521C1.32 -2.488 1.22 -2.384 1.218 -2.385C1.216 -2.387 1.129 -2.802 1.124 -2.836C1.103 -2.961 1.099 -3.077 1.111 -3.196C1.128 -3.349 1.175 -3.5 1.246 -3.625C1.263 -3.655 1.3 -3.709 1.324 -3.74C1.352 -3.775 1.407 -3.83 1.439 -3.856C1.507 -3.91 1.586 -3.95 1.646 -3.96C1.673 -3.964 1.708 -3.963 1.735 -3.956ZM1.228 -1.656C1.239 -1.604 1.355 -1.06 1.357 -1.055C1.358 -1.051 1.357 -1.049 1.355 -1.049C1.348 -1.049 1.279 -1.023 1.245 -1.007C1.115 -0.946 1.001 -0.853 0.915 -0.736C0.891 -0.704 0.875 -0.678 0.851 -0.634C0.818 -0.574 0.801 -0.531 0.78 -0.458C0.758 -0.381 0.746 -0.281 0.751 -0.2C0.759 -0.067 0.792 0.047 0.857 0.162C0.908 0.252 0.993 0.347 1.08 0.409C1.136 0.449 1.223 0.495 1.258 0.503C1.301 0.512 1.341 0.472 1.331 0.429C1.326 0.406 1.318 0.396 1.285 0.376C1.196 0.321 1.125 0.238 1.086 0.14C1.04 0.024 1.038 -0.111 1.081 -0.226C1.108 -0.3 1.148 -0.363 1.2 -0.417C1.256 -0.475 1.316 -0.518 1.388 -0.549C1.413 -0.56 1.458 -0.576 1.459 -0.574C1.459 -0.574 1.492 -0.422 1.531 -0.237C1.571 -0.053 1.64 0.271 1.685 0.481C1.73 0.692 1.767 0.866 1.767 0.867C1.767 0.873 1.672 0.898 1.614 0.908C1.567 0.916 1.54 0.919 1.493 0.92C1.342 0.926 1.204 0.9 1.066 0.841C0.866 0.756 0.696 0.609 0.576 0.417C0.48 0.265 0.422 0.088 0.403 -0.103C0.399 -0.141 0.399 -0.292 0.403 -0.333C0.422 -0.535 0.487 -0.733 0.593 -0.909C0.658 -1.018 0.832 -1.248 0.987 -1.431C1.066 -1.524 1.215 -1.691 1.22 -1.691C1.22 -1.691 1.224 -1.675 1.228 -1.656ZM1.705 -0.595C1.839 -0.58 1.967 -0.525 2.071 -0.438C2.106 -0.409 2.157 -0.357 2.179 -0.328C2.222 -0.27 2.258 -0.208 2.283 -0.144C2.33 -0.025 2.345 0.114 2.324 0.239C2.291 0.435 2.18 0.613 2.011 0.74C1.985 0.759 1.942 0.787 1.911 0.805L1.901 0.811L1.885 0.736C1.876 0.695 1.866 0.646 1.862 0.627C1.858 0.609 1.798 0.328 1.729 0.003C1.659 -0.321 1.602 -0.589 1.601 -0.593L1.6 -0.599L1.638 -0.599C1.659 -0.599 1.689 -0.597 1.705 -0.595Z"

@Suppress("ktlint:standard:max-line-length")
private const val BASS_CLEF_PATH = "M0.954 -1.068C0.78 -1.057 0.609 -1.007 0.464 -0.924C0.26 -0.807 0.114 -0.631 0.053 -0.428C0.043 -0.394 0.034 -0.353 0.029 -0.314C0.024 -0.278 0.024 -0.187 0.029 -0.151C0.044 -0.043 0.082 0.05 0.143 0.125C0.207 0.205 0.294 0.262 0.387 0.284C0.439 0.296 0.514 0.299 0.562 0.29C0.679 0.27 0.778 0.206 0.843 0.108C0.874 0.06 0.895 0.008 0.906 -0.051C0.912 -0.079 0.914 -0.147 0.91 -0.175C0.887 -0.364 0.745 -0.511 0.557 -0.539C0.527 -0.544 0.471 -0.544 0.439 -0.539C0.382 -0.531 0.326 -0.511 0.28 -0.481C0.27 -0.475 0.262 -0.471 0.262 -0.471C0.262 -0.472 0.267 -0.483 0.272 -0.497C0.299 -0.559 0.333 -0.616 0.377 -0.672C0.399 -0.7 0.445 -0.748 0.474 -0.772C0.572 -0.858 0.699 -0.921 0.83 -0.95C0.89 -0.963 0.941 -0.969 1.006 -0.971C1.16 -0.976 1.302 -0.93 1.427 -0.836C1.578 -0.722 1.684 -0.542 1.734 -0.315C1.755 -0.218 1.763 -0.15 1.766 -0.031C1.773 0.224 1.744 0.429 1.667 0.659C1.59 0.89 1.468 1.112 1.302 1.323C1.195 1.459 1.038 1.616 0.895 1.731C0.651 1.925 0.383 2.075 0.071 2.191C0.032 2.206 0.02 2.211 0.014 2.216C0.004 2.226 0 2.237 0 2.252C0 2.281 0.019 2.3 0.048 2.3C0.062 2.3 0.141 2.284 0.216 2.265C0.538 2.185 0.827 2.065 1.101 1.897C1.308 1.771 1.519 1.599 1.677 1.43C1.751 1.35 1.8 1.292 1.861 1.211C1.921 1.131 1.955 1.08 2.005 0.997C2.066 0.896 2.124 0.78 2.167 0.674C2.26 0.445 2.312 0.199 2.312 -0.005C2.312 -0.063 2.31 -0.095 2.303 -0.144C2.276 -0.327 2.202 -0.494 2.083 -0.639C2.012 -0.726 1.907 -0.815 1.807 -0.874C1.694 -0.941 1.588 -0.984 1.453 -1.019C1.303 -1.058 1.106 -1.077 0.954 -1.068ZM2.604 -0.659C2.591 -0.656 2.566 -0.647 2.554 -0.641C2.518 -0.621 2.492 -0.588 2.479 -0.548C2.473 -0.532 2.473 -0.529 2.473 -0.5C2.473 -0.471 2.473 -0.468 2.479 -0.452C2.496 -0.401 2.533 -0.364 2.584 -0.347C2.6 -0.341 2.603 -0.341 2.632 -0.341C2.661 -0.341 2.664 -0.341 2.68 -0.347C2.731 -0.364 2.768 -0.401 2.785 -0.452C2.791 -0.468 2.791 -0.471 2.791 -0.5C2.791 -0.529 2.791 -0.532 2.785 -0.548C2.769 -0.599 2.731 -0.636 2.681 -0.653C2.667 -0.658 2.661 -0.658 2.637 -0.659C2.622 -0.659 2.607 -0.659 2.604 -0.659ZM2.604 0.341C2.591 0.344 2.566 0.353 2.554 0.359C2.518 0.379 2.492 0.412 2.479 0.452C2.473 0.468 2.473 0.471 2.473 0.5C2.473 0.529 2.473 0.532 2.479 0.548C2.496 0.599 2.533 0.636 2.584 0.653C2.6 0.659 2.603 0.659 2.632 0.659C2.661 0.659 2.664 0.659 2.68 0.653C2.731 0.636 2.768 0.599 2.785 0.548C2.791 0.532 2.791 0.529 2.791 0.5C2.791 0.471 2.791 0.468 2.785 0.452C2.769 0.401 2.731 0.364 2.681 0.347C2.667 0.342 2.661 0.342 2.637 0.341C2.622 0.341 2.607 0.341 2.604 0.341Z"

// Filled gonville note head (headcrotchet): skewed ellipse, radii in staff spaces.
private const val NOTE_HEAD_RADIUS_X = 0.64f
private const val NOTE_HEAD_RADIUS_Y = 0.568f
private const val NOTE_HEAD_SKEW = 0.3f

/** The gonville accidentals are placed such that the note line is one staff space above
 * the text baseline, and one staff space equals this fraction of the font size.
 */
private const val ACCIDENTAL_STAFF_SPACE_PER_EM = 0.308f

/** Largest number of sharps or flats of a key signature. */
const val MAX_KEY_SIGNATURE = 7

private val SHARP_ORDER =
    listOf(BaseNote.F, BaseNote.C, BaseNote.G, BaseNote.D, BaseNote.A, BaseNote.E, BaseNote.B)
private val FLAT_ORDER = SHARP_ORDER.reversed()

// Staff positions of the key signature accidentals in the treble staff, in the order of
// SHARP_ORDER/FLAT_ORDER; the bass staff uses the same pattern two octaves lower.
private val TREBLE_SHARP_POSITIONS = listOf(38, 35, 39, 36, 33, 37, 34)
private val TREBLE_FLAT_POSITIONS = listOf(34, 37, 33, 36, 32, 35, 31)
private const val KEY_SIGNATURE_SPACING = 1.1f // staff spaces per accidental

private const val NATURAL_SYMBOL = "\uE107"

/** Modifier which a key signature applies to a base note.
 * @param keySignature Number of sharps (> 0) or flats (< 0).
 */
fun keySignatureModifier(base: BaseNote, keySignature: Int): NoteModifier = when {
    keySignature > 0 && base in SHARP_ORDER.take(keySignature) -> NoteModifier.Sharp
    keySignature < 0 && base in FLAT_ORDER.take(-keySignature) -> NoteModifier.Flat
    else -> NoteModifier.None
}

/** Note as written on the staff.
 * @param note Spelling of the note (base and modifier are used, enharmonic is not).
 * @param showAccidental True if an accidental must be drawn; for notes without modifier this
 *   is a natural sign, cancelling the key signature.
 */
data class SpelledNote(val note: MusicalNote, val showAccidental: Boolean)

private fun NoteModifier.isSharp() = this == NoteModifier.Sharp || this == NoteModifier.SharpSharp
private fun NoteModifier.isFlat() = this == NoteModifier.Flat || this == NoteModifier.FlatFlat

/** Enharmonic preference ([NotePrintOptions.useEnharmonic]) which matches the accidentals of a
 * key signature: sharp names for sharp keys, flat names for flat keys.
 * @param keySignature Number of sharps (> 0) or flats (< 0).
 * @param noteNames Note names of the musical scale, used to find out whether the primary or
 *   the enharmonic spelling is the sharp one.
 * @return Preference or null for keySignature 0 or if the note names have no pair of sharp and
 *   flat spellings.
 */
fun useEnharmonicForKey(keySignature: Int, noteNames: Iterable<MusicalNote>): Boolean? {
    if (keySignature == 0) {
        return null
    }
    val sharpFlatPair = noteNames.firstOrNull {
        (it.modifier.isSharp() && it.enharmonicModifier.isFlat()) ||
            (it.modifier.isFlat() && it.enharmonicModifier.isSharp())
    } ?: return null
    val primaryIsSharp = sharpFlatPair.modifier.isSharp()
    return primaryIsSharp != (keySignature > 0)
}

/** Choose the spelling of a note for the staff.
 * @param useEnharmonic Enharmonic preference, used if there is no key signature and when the
 *   key signature does not decide between two spellings.
 * @param keySignature Number of sharps (> 0) or flats (< 0) or null if the staff has no key
 *   signature (e.g. for temperaments, which do not have 12 notes per octave).
 */
fun spellNote(note: MusicalNote, useEnharmonic: Boolean, keySignature: Int?): SpelledNote {
    val preferred = if (useEnharmonic) note.switchEnharmonic() else note
    if (keySignature == null) {
        return SpelledNote(preferred, preferred.modifier != NoteModifier.None)
    }

    val candidates = if (note.enharmonicBase == BaseNote.None) {
        listOf(note)
    } else {
        listOf(preferred, preferred.switchEnharmonic())
    }

    val spelled =
        candidates.firstOrNull { it.modifier == keySignatureModifier(it.base, keySignature) }
            ?: candidates.firstOrNull {
                when {
                    keySignature > 0 -> it.modifier.isSharp()
                    keySignature < 0 -> it.modifier.isFlat()
                    else -> false
                }
            }
            ?: candidates.first()
    return SpelledNote(
        spelled,
        spelled.modifier != keySignatureModifier(spelled.base, keySignature)
    )
}

/** Position of the note on a staff, counted in diatonic steps with C0 = 0.
 * @return Staff position or null if the note has no base note or no octave.
 */
fun MusicalNote.staffPosition(): Int? {
    if (base == BaseNote.None || octave == Int.MAX_VALUE) {
        return null
    }
    return 7 * (octave + octaveOffset) + base.ordinal
}

/** Placement of a note on the staff.
 * @param position Staff position, where the note head is drawn.
 * @param octaveMark Number of octaves the note is written above (> 0) or below (< 0) its
 *   sounding pitch. This is marked at the clef: below the (lowest) clef for > 0, above the
 *   (highest) clef for < 0.
 */
data class StaffPlacement(val position: Int, val octaveMark: Int)

/** Place a note on the staff, shifting it by octaves if it is out of the drawn range.
 * @param soundingPosition Staff position of the sounding pitch.
 */
fun staffPlacement(soundingPosition: Int, clef: StaffClef): StaffPlacement {
    val layout = clef.layout()
    var position = soundingPosition + 7 * layout.writtenOctaves
    var octaveMark = layout.writtenOctaves
    while (position > layout.highestPosition) {
        position -= 7
        --octaveMark
    }
    while (position < layout.lowestPosition) {
        position += 7
        ++octaveMark
    }
    return StaffPlacement(position, octaveMark)
}

/** Positions of the ledger lines which are needed for a note at the given position. */
fun ledgerLinePositions(position: Int, clef: StaffClef): IntProgression {
    val staves = clef.layout().staves
    val top = staves.first().topLine
    val bottom = staves.last().bottomLine
    return when {
        position >= top + 2 -> (top + 2)..position step 2

        position <= bottom - 2 -> (bottom - 2) downTo position step 2

        // middle C between treble and bass staff
        staves.size > 1 && position == MIDDLE_C -> MIDDLE_C..MIDDLE_C

        else -> IntRange.EMPTY
    }
}

private fun parseClefPath(pathData: String): Path =
    PathParser().parsePathString(pathData).toPath().apply {
        fillType = PathFillType.EvenOdd
    }

private fun createNoteHeadPath() = Path().apply {
    addOval(Rect(-NOTE_HEAD_RADIUS_X, -NOTE_HEAD_RADIUS_Y, NOTE_HEAD_RADIUS_X, NOTE_HEAD_RADIUS_Y))
    // gonville y points downwards, so this tilts the head upwards to the right
    transform(Matrix().apply { this[1, 0] = -NOTE_HEAD_SKEW })
}

/** Staff (grand staff or single staff, see [StaffClef]) showing a single note.
 * @param note Note to be shown or null to show an empty staff.
 * @param modifier Modifier.
 * @param tuningState Tuning state, which defines the color of the note.
 * @param notePrintOptions Note print options, used for the note name label and for the
 *   spelling when the key signature does not decide it.
 * @param staffColor Color of staff lines and clefs.
 * @param inTuneColor Note color when the note is in tune.
 * @param outOfTuneColor Note color when the note is out of tune.
 * @param unknownTuningColor Note color when the tuning state is unknown.
 * @param fontSize Font size of note name label.
 * @param outline Outline of the staff window.
 * @param keySignature Number of sharps (> 0) or flats (< 0) of the key signature or null
 *   for a staff without key signature.
 * @param onKeySignatureChange If not null (and keySignature is not null), buttons for adding
 *   sharps/flats are shown, which call this with the new key signature.
 * @param staffClef Clef setup of the staff.
 * @param onStaffClefChange If not null, a button for choosing the clef setup is shown, which
 *   calls this with the chosen setup.
 * @param onClick Callback when the staff is clicked.
 */
@Composable
fun NoteStaff(
    note: MusicalNote?,
    modifier: Modifier = Modifier,
    tuningState: TuningState = TuningState.Unknown,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    staffColor: Color = MaterialTheme.colorScheme.onSurface,
    inTuneColor: Color = MaterialTheme.tunerColors.positive,
    outOfTuneColor: Color = MaterialTheme.tunerColors.negative,
    unknownTuningColor: Color = MaterialTheme.colorScheme.primary,
    fontSize: TextUnit = 16.sp,
    outline: PlotWindowOutline = PlotWindowOutline(),
    keySignature: Int? = null,
    onKeySignatureChange: ((Int) -> Unit)? = null,
    staffClef: StaffClef = StaffClef.Grand,
    onStaffClefChange: ((StaffClef) -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    val resources = LocalContext.current.resources
    val textMeasurer = rememberTextMeasurer()
    val trebleClef = remember { parseClefPath(TREBLE_CLEF_PATH) }
    val bassClef = remember { parseClefPath(BASS_CLEF_PATH) }
    val noteHeadPath = remember { createNoteHeadPath() }
    val outlineColor = outline.color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    val noteColor = when (tuningState) {
        TuningState.InTune -> inTuneColor
        TuningState.TooLow, TuningState.TooHigh -> outOfTuneColor
        else -> unknownTuningColor
    }
    val spelledNote = remember(note, notePrintOptions, keySignature) {
        note?.let { spellNote(it, notePrintOptions.useEnharmonic, keySignature) }
    }
    val placement = remember(spelledNote, staffClef) {
        spelledNote?.note?.staffPosition()?.let { staffPlacement(it, staffClef) }
    }
    val noteName = remember(spelledNote, notePrintOptions, fontSize, resources) {
        spelledNote?.note?.asAnnotatedString(
            notePrintOptions.copy(useEnharmonic = false),
            fontSize,
            FontWeight.Bold,
            true,
            resources
        )
    }
    val numKeyAccidentals = abs(keySignature ?: 0)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
        ) {
            val outlineWidth = outline.lineWidth.toPx()
            val cornerRadius = CornerRadius(outline.cornerRadius.toPx())
            val clip = Path().apply {
                addRoundRect(RoundRect(Rect(Offset.Zero, size), cornerRadius))
            }
            clipPath(clip) {
                val staffLayout = staffClef.layout()
                val staffSpace = min(
                    size.height / staffLayout.staffSpacesVisible,
                    size.width / (9f + numKeyAccidentals * KEY_SIGNATURE_SPACING)
                )
                val lineWidth = 0.1f * staffSpace
                val left = 0.5f * staffSpace
                val right = size.width - 0.5f * staffSpace
                val centerY = 0.5f * size.height
                fun y(position: Int) =
                    centerY - 0.5f * staffSpace * (position - staffLayout.centerPosition)

                for (staff in staffLayout.staves) {
                    for (position in staff.bottomLine..staff.topLine step 2) {
                        drawLine(
                            staffColor,
                            Offset(left, y(position)),
                            Offset(right, y(position)),
                            lineWidth
                        )
                    }
                }
                drawLine(
                    staffColor,
                    Offset(left, y(staffLayout.staves.first().topLine) - 0.5f * lineWidth),
                    Offset(left, y(staffLayout.staves.last().bottomLine) + 0.5f * lineWidth),
                    lineWidth
                )
                val clefX = left + 0.5f * staffSpace
                for (staff in staffLayout.staves) {
                    val clefPath = if (staff.glyph == ClefGlyph.Treble) trebleClef else bassClef
                    drawClef(clefPath, clefX, y(staff.clefLine), staffSpace, staffColor)
                }

                val keySignatureX = clefX + (BASS_CLEF_WIDTH + 0.6f) * staffSpace
                if (keySignature != null && keySignature != 0) {
                    val symbol = if (keySignature > 0) {
                        NoteModifier.Sharp.accidentalSymbols()
                    } else {
                        NoteModifier.Flat.accidentalSymbols()
                    }
                    val treblePositions = if (keySignature > 0) {
                        TREBLE_SHARP_POSITIONS
                    } else {
                        TREBLE_FLAT_POSITIONS
                    }
                    for (i in 0 until numKeyAccidentals) {
                        val rightX = keySignatureX + (i + 1) * KEY_SIGNATURE_SPACING * staffSpace
                        for (staff in staffLayout.staves) {
                            drawAccidental(
                                textMeasurer,
                                symbol,
                                rightX,
                                y(treblePositions[i] + staff.keySignatureOffset),
                                staffSpace,
                                staffColor
                            )
                        }
                    }
                }

                val noteX = maxOf(
                    keySignatureX + (numKeyAccidentals * KEY_SIGNATURE_SPACING + 3.5f) * staffSpace,
                    0.45f * size.width
                )
                if (placement != null) {
                    val noteY = y(placement.position)
                    for (position in ledgerLinePositions(placement.position, staffClef)) {
                        drawLine(
                            staffColor,
                            Offset(noteX - 1.1f * staffSpace, y(position)),
                            Offset(noteX + 1.1f * staffSpace, y(position)),
                            lineWidth
                        )
                    }
                    translate(noteX, noteY) {
                        withTransform({ scale(staffSpace, staffSpace, Offset.Zero) }) {
                            drawPath(noteHeadPath, noteColor)
                        }
                    }
                    if (spelledNote?.showAccidental == true) {
                        val symbols = if (spelledNote.note.modifier == NoteModifier.None) {
                            NATURAL_SYMBOL
                        } else {
                            spelledNote.note.modifier.accidentalSymbols()
                        }
                        drawAccidental(
                            textMeasurer,
                            symbols,
                            noteX - 1.1f * staffSpace,
                            noteY,
                            staffSpace,
                            noteColor
                        )
                    }
                }

                // Octave mark of the clef: the clef's own transposition (tenor), or the shift of
                // a note out of range, which is highlighted in the note color.
                val octaveMark = placement?.octaveMark ?: staffLayout.writtenOctaves
                if (octaveMark != 0) {
                    val staff = if (octaveMark > 0) {
                        staffLayout.staves.last()
                    } else {
                        staffLayout.staves.first()
                    }
                    val clefEdge = if (octaveMark > 0) staff.glyph.bottom else staff.glyph.top
                    val isClefOwnMark = octaveMark == staffLayout.writtenOctaves
                    drawOctaveClefMark(
                        textMeasurer,
                        octaves = abs(octaveMark),
                        above = octaveMark < 0,
                        centerX = clefX + 0.5f * staff.glyph.width * staffSpace,
                        clefEdgeY = y(staff.clefLine) + clefEdge * staffSpace,
                        staffSpace = staffSpace,
                        color = if (isClefOwnMark) staffColor else noteColor
                    )
                }
                if (noteName != null) {
                    val layout = textMeasurer.measure(noteName, TextStyle(color = noteColor))
                    val labelY = if (placement !=
                        null
                    ) {
                        y(placement.position)
                    } else {
                        centerY
                    }
                    drawText(
                        layout,
                        topLeft = Offset(
                            noteX + 1.5f * staffSpace,
                            (labelY - 0.5f * layout.size.height).coerceIn(
                                0f,
                                maxOf(0f, size.height - layout.size.height)
                            )
                        )
                    )
                }
            }
            drawRoundRect(
                outlineColor,
                topLeft = Offset(0.5f * outlineWidth, 0.5f * outlineWidth),
                size = Size(size.width - outlineWidth, size.height - outlineWidth),
                cornerRadius = cornerRadius,
                style = Stroke(outlineWidth)
            )
        }
        if (onStaffClefChange != null) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                StaffClefButton(staffClef, onStaffClefChange)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (keySignature != null && onKeySignatureChange != null) {
                KeySignatureButton(
                    symbol = NoteModifier.Flat.accidentalSymbols(),
                    contentDescription = stringResource(R.string.key_signature_flat),
                    enabled = keySignature > -MAX_KEY_SIGNATURE,
                    onClick = { onKeySignatureChange(keySignature - 1) }
                )
                KeySignatureButton(
                    symbol = NoteModifier.Sharp.accidentalSymbols(),
                    contentDescription = stringResource(R.string.key_signature_sharp),
                    enabled = keySignature < MAX_KEY_SIGNATURE,
                    onClick = { onKeySignatureChange(keySignature + 1) }
                )
            }
        }
    }
}

private val ClefGlyph.width get() = if (this ==
    ClefGlyph.Treble
) {
    TREBLE_CLEF_WIDTH
} else {
    BASS_CLEF_WIDTH
}
private val ClefGlyph.top get() = if (this == ClefGlyph.Treble) TREBLE_CLEF_TOP else BASS_CLEF_TOP
private val ClefGlyph.bottom get() =
    if (this == ClefGlyph.Treble) TREBLE_CLEF_BOTTOM else BASS_CLEF_BOTTOM

private fun StaffClef.labelResource() = when (this) {
    StaffClef.Grand -> R.string.staff_clef_grand
    StaffClef.Treble -> R.string.staff_clef_treble
    StaffClef.Tenor -> R.string.staff_clef_tenor
    StaffClef.Bass -> R.string.staff_clef_bass
}

@Composable
private fun StaffClefButton(staffClef: StaffClef, onStaffClefChange: (StaffClef) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(R.string.staff_clef)
    Box {
        FilledTonalIconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp).semantics { contentDescription = description }
        ) {
            ClefIcon(staffClef, LocalContentColor.current, Modifier.size(26.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StaffClef.entries.forEach { clef ->
                DropdownMenuItem(
                    text = { Text(stringResource(clef.labelResource())) },
                    leadingIcon = {
                        ClefIcon(clef, LocalContentColor.current, Modifier.size(32.dp))
                    },
                    trailingIcon = { RadioButton(selected = clef == staffClef, onClick = null) },
                    onClick = {
                        expanded = false
                        onStaffClefChange(clef)
                    }
                )
            }
        }
    }
}

/** Small picture of the clef(s) of a staff setup. */
@Composable
private fun ClefIcon(clef: StaffClef, color: Color, modifier: Modifier = Modifier) {
    val trebleClef = remember { parseClefPath(TREBLE_CLEF_PATH) }
    val bassClef = remember { parseClefPath(BASS_CLEF_PATH) }
    val textMeasurer = rememberTextMeasurer()
    val trebleHeight = TREBLE_CLEF_BOTTOM - TREBLE_CLEF_TOP
    val bassHeight = BASS_CLEF_BOTTOM - BASS_CLEF_TOP
    Canvas(modifier) {
        when (clef) {
            StaffClef.Treble, StaffClef.Tenor -> {
                // leave room for the octave mark below the tenor clef
                val markHeight = if (clef == StaffClef.Tenor) 1.8f else 0f
                val staffSpace = size.height / (trebleHeight + markHeight)
                val lineY = -TREBLE_CLEF_TOP * staffSpace
                drawClef(
                    trebleClef,
                    0.5f * (size.width - TREBLE_CLEF_WIDTH * staffSpace),
                    lineY,
                    staffSpace,
                    color
                )
                if (clef == StaffClef.Tenor) {
                    drawOctaveClefMark(
                        textMeasurer,
                        octaves = 1,
                        above = false,
                        centerX = 0.5f * size.width,
                        clefEdgeY = lineY + TREBLE_CLEF_BOTTOM * staffSpace,
                        staffSpace = staffSpace,
                        color = color
                    )
                }
            }

            StaffClef.Bass -> {
                val staffSpace = min(size.height / bassHeight, size.width / BASS_CLEF_WIDTH)
                drawClef(
                    bassClef,
                    0.5f * (size.width - BASS_CLEF_WIDTH * staffSpace),
                    0.5f * size.height - 0.5f * (BASS_CLEF_TOP + BASS_CLEF_BOTTOM) * staffSpace,
                    staffSpace,
                    color
                )
            }

            StaffClef.Grand -> {
                val gap = 0.3f
                val totalWidth = TREBLE_CLEF_WIDTH + gap + BASS_CLEF_WIDTH
                val staffSpace = min(size.height / trebleHeight, size.width / totalWidth)
                val x = 0.5f * (size.width - totalWidth * staffSpace)
                drawClef(
                    trebleClef,
                    x,
                    0.5f * size.height - 0.5f * (TREBLE_CLEF_TOP + TREBLE_CLEF_BOTTOM) * staffSpace,
                    staffSpace,
                    color
                )
                drawClef(
                    bassClef,
                    x + (TREBLE_CLEF_WIDTH + gap) * staffSpace,
                    0.5f * size.height - 0.5f * (BASS_CLEF_TOP + BASS_CLEF_BOTTOM) * staffSpace,
                    staffSpace,
                    color
                )
            }
        }
    }
}

@Composable
private fun KeySignatureButton(
    symbol: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp).semantics { this.contentDescription = contentDescription }
    ) {
        Text(symbol, fontFamily = musicalSymbolFont, fontSize = 22.sp)
    }
}

private fun DrawScope.drawClef(
    clef: Path,
    x: Float,
    lineY: Float,
    staffSpace: Float,
    color: Color
) {
    translate(x, lineY) {
        withTransform({ scale(staffSpace, staffSpace, Offset.Zero) }) {
            drawPath(clef, color)
        }
    }
}

/** Draw accidental glyphs, such that they end at [rightX] and are vertically aligned to [noteY]. */
private fun DrawScope.drawAccidental(
    textMeasurer: TextMeasurer,
    symbols: String,
    rightX: Float,
    noteY: Float,
    staffSpace: Float,
    color: Color
) {
    val fontSizePx = staffSpace / ACCIDENTAL_STAFF_SPACE_PER_EM
    val layout = textMeasurer.measure(
        symbols,
        TextStyle(color = color, fontFamily = musicalSymbolFont, fontSize = fontSizePx.toSp())
    )
    val baselineY = noteY + staffSpace
    drawText(layout, topLeft = Offset(rightX - layout.size.width, baselineY - layout.firstBaseline))
}

/** Draw the octave mark of an octave clef (8, 15, ...).
 * @param octaves Number of octaves (1 -> 8, 2 -> 15, ...).
 * @param above True to draw the mark above the clef (notes sound higher than written), false
 *   to draw it below (notes sound lower than written).
 * @param clefEdgeY Top edge of the clef if [above], else bottom edge.
 */
private fun DrawScope.drawOctaveClefMark(
    textMeasurer: TextMeasurer,
    octaves: Int,
    above: Boolean,
    centerX: Float,
    clefEdgeY: Float,
    staffSpace: Float,
    color: Color
) {
    val layout = textMeasurer.measure(
        (7 * octaves + 1).toString(),
        TextStyle(
            color = color,
            fontSize = (1.4f * staffSpace).toSp(),
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )
    )
    val top = if (above) clefEdgeY - layout.size.height else clefEdgeY
    drawText(layout, topLeft = Offset(centerX - 0.5f * layout.size.width, top))
}

@Preview(widthDp = 300, heightDp = 250, showBackground = true)
@Composable
private fun NoteStaffPreview() {
    TunerTheme {
        NoteStaff(
            note = MusicalNote(BaseNote.F, NoteModifier.Sharp, 4),
            modifier = Modifier.size(300.dp, 250.dp),
            tuningState = TuningState.TooLow
        )
    }
}

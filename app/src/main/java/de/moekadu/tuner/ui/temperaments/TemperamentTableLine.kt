package de.moekadu.tuner.ui.temperaments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments2.centsToFrequency
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.temperaments2.ratioToCents
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.Note
import de.moekadu.tuner.ui.notes.NoteEditorSimple
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.roundToInt

private fun stringToRatio(value: String): RationalNumber? {
    if (value.contains('/')) {
        val values = value.split('/')
        val numerator = values[0].trim().toIntOrNull()
        val denominator = values[1].trim().toIntOrNull()
        return if (numerator != null && denominator != null && numerator > 0 && denominator > 0)
            RationalNumber(numerator, denominator)
        else
            null
    }
    return null
}

private fun stringToCents(centOrRatio: String): Double? {
    val cents = centOrRatio.replace(",", ".").trim().toDoubleOrNull()
    return when {
        cents == null -> null
        cents < 0.0 -> null
        else -> cents
    }
}

private fun centToRatio(
    value: Double,
    maximumDenominator: Int,
    maximumAllowedToleranceInCents: Double
    ): RationalNumber? {
    // check a few possible ratios if they match the cents

    val ratioMin = centsToFrequency(value - maximumAllowedToleranceInCents, 1.0)
    val ratioMax = centsToFrequency(value + maximumAllowedToleranceInCents, 1.0)
    val ratio = centsToFrequency(value, 1.0)

    for (possibleDenominator in 1 .. maximumDenominator) {
        val possibleNumerator = (ratio * possibleDenominator).roundToInt()
        val possibleRatio = possibleNumerator / possibleDenominator
        if (possibleRatio > ratioMin && possibleRatio < ratioMax)
            return RationalNumber(possibleNumerator, possibleDenominator)
    }
    return null
}

//private fun checkCentOrRatioValidity(centOrRatio: String): Boolean {
//    // check ratios
//    if (centOrRatio.contains('/')) {
//        val values = centOrRatio.split('/')
//        values[0].trim().toIntOrNull() ?: return false
//        values[1].trim().toIntOrNull() ?: return false
//        return true
//    }
//    // check cents
//    return centOrRatio.replace(",", ".").trim().toDoubleOrNull() != null
//}

class TemperamentTableLineState(
    note: MusicalNote?,
    cent: Double?,
    ratio: RationalNumber?, // if there is a ratio, we ignore the cent value!
    val isOctaveLine: Boolean,
    decreasingValueError: Boolean,
    duplicateNoteError: Boolean
) {
    enum class NoteEditorState {
        Off,
        Standard,
        Enharmonic
    }
    var note by mutableStateOf(note)

    private var _centOrRatio by mutableStateOf<String?>(null)
    val centOrRatio get() = _centOrRatio

    private var _invalidValueError by mutableStateOf(cent==null && ratio == null)
    val invalidValueError get() = _invalidValueError

    private var _decreasingValueError by mutableStateOf(decreasingValueError)
    val decreasingValueError get() = _decreasingValueError

    private var _duplicateNoteError by mutableStateOf(duplicateNoteError)
    val duplicateNoteError get() = _duplicateNoteError

    private var _noteEditorState by mutableStateOf(NoteEditorState.Off)
    val noteEditorState get() = _noteEditorState

    var cent = cent
        private set
    var ratio = ratio
        private set

    fun changeCentOrRatio(value: String) {
        _centOrRatio = value

        val possibleRatio = stringToRatio(value)
        val possibleCents = stringToCents(value)
        if (possibleRatio != null) {
            ratio = possibleRatio
            cent = ratioToCents(possibleRatio.toDouble())
//            Log.v("Tuner", "TemperamentLineTable.changeCentOrRatio: ratio=$ratio, cent=$cent")
            _invalidValueError = false
        } else if (possibleCents != null) {
            ratio = null
            cent = possibleCents
            _invalidValueError = false
        } else {
            ratio = null
            cent = null
            _invalidValueError = true
        }
    }

    fun changeDecreasingValueError(error: Boolean) {
        _decreasingValueError = error
    }

    fun changeDuplicateNoteError(error: Boolean) {
        _duplicateNoteError = error
    }
    
    fun setNoteEditor(state: NoteEditorState){
        _noteEditorState = state
    }

    fun obtainRatio(): RationalNumber? {
        return ratio
            ?: cent?.let { centToRatio(it, 5, 0.1) }
    }
    fun obtainCent(): Double? {
        return ratio?.let { ratioToCents(it.toDouble()) } ?: cent
    }
}

@Composable
private fun ClickableNote(
    note: MusicalNote?,
    selected: Boolean,
    onClick: () -> Unit,
    notePrintOptions: NotePrintOptions,
    maximumNoteWidth: Dp,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        onClick = if (enabled) { onClick } else { {} },
        color = when {
            selected && isError -> MaterialTheme.colorScheme.errorContainer
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(maximumNoteWidth + 12.dp)
                .heightIn(40.dp, Dp.Unspecified),
            contentAlignment = Alignment.Center
        ) {
            val contentColor = when {
                !enabled -> LocalContentColor.current.copy(alpha = 0.38f)
                isError && !selected -> MaterialTheme.colorScheme.error
                else -> LocalContentColor.current
            }
            if (note != null && note.base != BaseNote.None) {
                Note(
                    note,
                    notePrintOptions = notePrintOptions,
                    withOctave = true,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            } else {
                Text(
                    "-",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }
        }
    }
}


@Composable
fun TemperamentTableLine(
    lineNumber: Int,
    state: TemperamentTableLineState,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onValueChange: (value: String) -> Unit = {},
    onNoteNameClicked: (enharmonic: Boolean) -> Unit = {},
    onNoteEditorCloseClicked: () -> Unit = {},
    onChangeNote: (MusicalNote?) -> Unit = {}
) {
    val notePrintOptionsDefault = remember(notePrintOptions) {
        notePrintOptions.copy(sharpFlatPreference = NotePrintOptions.SharpFlatPreference.None)
    }

    val density = LocalDensity.current
    val noteWidth = with(density) { 45.sp.toDp()}

    val contentAlpha = if (state.isOctaveLine) 0.38f else 1f
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)

    val isRatio = state.centOrRatio?.contains("/") == true
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration).get(0)

    LaunchedEffect(Unit) {
        if (state.centOrRatio == null) {
            state.ratio?.let {
                onValueChange("${it.numerator} / ${it.denominator}")
            } ?: state.cent?.let { cent ->
                onValueChange(String.format(locale, "%.1f", cent))
            }
        }
    }

    Surface(modifier = modifier) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                modifier = Modifier
                    .padding(bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // TODO: don't use a switch note, but notePrintOptions with enharmonic preference
                        //  -> However, this needs to refactor the notePrintOptions, and remove this
                        //     whole sharp/flat preference to a simple "useEnharmonic"
                        val noteSwitched by remember {derivedStateOf {
                            state.note?.switchEnharmonic(true)
                        }}
                        Text(
                            "${lineNumber + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .width(32.dp)
                                .padding(start = 16.dp),
                            textAlign = TextAlign.Center,
                            color = contentColor
                        )
                        ClickableNote(
                            note = state.note,
                            selected = (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard),
                            onClick = { onNoteNameClicked(false) },
                            notePrintOptions = notePrintOptionsDefault,
                            maximumNoteWidth = noteWidth,
                            isError = state.duplicateNoteError,
                            enabled = !state.isOctaveLine
                        )

                        Text(
                            "/",
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                        ClickableNote(
                            note = noteSwitched,
                            selected = (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic),
                            onClick = { onNoteNameClicked(true) },
                            notePrintOptions = notePrintOptionsDefault,
                            maximumNoteWidth = noteWidth,
                            isError = state.duplicateNoteError,
                            enabled = !state.isOctaveLine
                        )
                    }

                }
                Spacer(modifier = Modifier.width(16.dp))

                TextField(
                    value = state.centOrRatio ?: "",
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            if (isRatio)
                                stringResource(id = R.string.ratio)
                            else
                                stringResource(id = R.string.cent_str)
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                    isError = state.invalidValueError || state.decreasingValueError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            AnimatedVisibility(visible = state.noteEditorState != TemperamentTableLineState.NoteEditorState.Off) {
                Column {
                    val resolvedBase by remember { derivedStateOf {
                        val n = state.note
                        if (n == null) {
                            BaseNote.A
                        } else if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard
                            && n.base != BaseNote.None) {
                            n.base
                        } else if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic
                            && n.enharmonicBase != BaseNote.None) {
                            n.enharmonicBase
                        } else if (n.base != BaseNote.None) {
                            n.base
                        } else if (n.enharmonicBase != BaseNote.None) {
                            n.enharmonicBase
                        } else {
                            BaseNote.A
                        }
                    } }

                    val resolvedModifier by remember { derivedStateOf {
                        val n = state.note
                        if (n == null) {
                            NoteModifier.None
                        } else if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard
                            && n.base != BaseNote.None
                        ) {
                            n.modifier
                        } else if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic
                            && n.enharmonicBase != BaseNote.None
                        ) {
                            n.enharmonicModifier
                        } else if (n.base != BaseNote.None) {
                            n.modifier
                        } else if (n.enharmonicBase != BaseNote.None) {
                            n.enharmonicModifier
                        } else {
                            NoteModifier.None
                        }
                    } }
                    val resolvedOffset by remember { derivedStateOf {
                        val n = state.note
                        if (n == null) {
                            0
                        } else if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard
                            && n.base != BaseNote.None
                        ) {
                            n.octaveOffset
                        } else if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic
                            && n.enharmonicBase != BaseNote.None
                        ) {
                            n.enharmonicOctaveOffset
                        } else if (n.base != BaseNote.None) {
                            n.octaveOffset
                        } else if (n.enharmonicBase != BaseNote.None) {
                            n.enharmonicOctaveOffset
                        } else {
                            0
                        }
                    } }
                    NoteEditorSimple(
                        base = resolvedBase,
                        noteModifier = resolvedModifier,
                        octaveOffset = resolvedOffset,
                        onNoteChange = { base, modifier, offset ->
//                            Log.v("Tuner", "TemperamentTableLine: noteEditorState=${state.noteEditorState}")
                            if (state.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard) {
                                onChangeNote(
                                    state.note?.copy(
                                        base = base,
                                        modifier = modifier,
                                        octaveOffset = offset
                                    )
                                )
                            } else {
                                onChangeNote(
                                    state.note?.copy(
                                        enharmonicBase = base,
                                        enharmonicModifier = modifier,
                                        enharmonicOctaveOffset = offset
                                    )
                                )
                            }
                        },
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier.padding(top = 4.dp, start=4.dp, end = 4.dp)
                    )
                    TextButton(
                        onClick = { onNoteEditorCloseClicked() },
                        modifier = Modifier.fillMaxWidth()
                        ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "close")
                    }
                }
            }

            AnimatedVisibility(visible = (
                    state.centOrRatio == null || state.centOrRatio == ""
                            || state.invalidValueError
                            || state.decreasingValueError
                    )
            ) {
                Text(
                    when {
                        state.centOrRatio == null || state.centOrRatio == "" -> {
                            stringResource(R.string.missing_value)
                        }
                        state.invalidValueError -> {
                            stringResource(R.string.invalid_value)
                        }
                        state.decreasingValueError -> {
                            stringResource(R.string.decreasing_value_error)
                        }
                        else -> ""
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            AnimatedVisibility(visible = (state.note == null || state.duplicateNoteError)
            ) {
                Text(
                    when {
                        state.note == null -> {
                            stringResource(R.string.missing_note_name)
                        }
                        state.duplicateNoteError -> {
                            stringResource(R.string.duplicate_note_name)
                        }
                        else -> ""
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            HorizontalDivider(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Preview(heightDp = 600, widthDp = 300, showBackground = true)
@Composable
private fun TemperamentTableLinePreview() {
    TunerTheme {
        val notes = remember { getSuitableNoteNames(12)!! }

        Column {
            notes.notes.forEachIndexed { index, note ->
                val state = remember {
                    TemperamentTableLineState(
                        note.copy(octave = 4),
                        index * 100.0,
                        if (index == 5) RationalNumber(1, 6) else null,
                        isOctaveLine = index == notes.size - 1,
                        decreasingValueError = index == 4 || index == 3,
                        duplicateNoteError = index == 1 || index == 3
                    )
                }

                TemperamentTableLine(
                    index,
                    state,
                    notePrintOptions = NotePrintOptions(notationType = NotationType.Solfege),
                    onValueChange = { state.changeCentOrRatio(it) },
                    onNoteNameClicked = { enharmonic ->
                        if ((!enharmonic && state.noteEditorState == TemperamentTableLineState.NoteEditorState.Standard)
                            || (enharmonic && state.noteEditorState == TemperamentTableLineState.NoteEditorState.Enharmonic)
                        ) {
                            state.setNoteEditor(TemperamentTableLineState.NoteEditorState.Off)
                        } else if (!enharmonic) {
                            state.setNoteEditor(TemperamentTableLineState.NoteEditorState.Standard)
                        } else {
                            state.setNoteEditor(TemperamentTableLineState.NoteEditorState.Enharmonic)
                        }
                    },
                    onNoteEditorCloseClicked = {
                        state.setNoteEditor(TemperamentTableLineState.NoteEditorState.Off)
                    },
                    onChangeNote = { state.note = it }
                )
            }
        }
    }
}
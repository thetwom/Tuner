package de.moekadu.tuner.ui.temperaments

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments2.centsToFrequency
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.temperaments2.ratioToCents
import de.moekadu.tuner.ui.notes.NotationType
import de.moekadu.tuner.ui.notes.Note
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

private fun checkCentOrRatioValidity(centOrRatio: String): Boolean {
    // check ratios
    if (centOrRatio.contains('/')) {
        val values = centOrRatio.split('/')
        values[0].trim().toIntOrNull() ?: return false
        values[1].trim().toIntOrNull() ?: return false
        return true
    }
    // check cents
    return centOrRatio.replace(",", ".").trim().toDoubleOrNull() != null
}

class TemperamentTableLineState(
    note: MusicalNote?,
    cent: Double?,
    ratio: RationalNumber?, // if there is a ratio, we ignore the cent value!
    isReferenceNote: Boolean,
    val isOctaveLine: Boolean
) {
    var note by mutableStateOf(note)
    private var _centOrRatio by mutableStateOf<String?>(null)

    val centOrRatio get() = _centOrRatio
    private var _hasError by mutableStateOf(cent==null && ratio == null)
    val hasError get() = _hasError

    var cent = cent
        private set
    var ratio = ratio
        private set
    var isReferenceNote by mutableStateOf(isReferenceNote)

    fun changeCentOrRatio(value: String) {
        _centOrRatio = value

        val possibleRatio = stringToRatio(value)
        val possibleCents = stringToCents(value)
        if (possibleRatio != null) {
            ratio = possibleRatio
            cent = possibleRatio.toDouble()
            _hasError = false
        } else if (possibleCents != null) {
            ratio = null
            cent = possibleCents
            _hasError = false
        } else {
            _hasError = true
        }
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
fun TemperamentTableLine(
    lineNumber: Int,
    state: TemperamentTableLineState,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onValueChange: (value: String) -> Unit = {},
    onNoteNameClicked: () -> Unit = {}
) {
    val notePrintOptionsDefault = remember(notePrintOptions) {
        notePrintOptions.copy(sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Sharp)
    }
    val notePrintOptionsOther = remember(notePrintOptions) {
        notePrintOptions.copy(sharpFlatPreference = NotePrintOptions.SharpFlatPreference.Flat)
    }

    val density = LocalDensity.current
    val noteWidth = with(density) { 45.sp.toDp()}
    
    val backgroundColor = when {
        //state.hasError -> MaterialTheme.colorScheme.errorContainer
        state.isReferenceNote -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentAlpha = if (state.isOctaveLine) 0.38f else 1f
    val contentColor = when {
        //state.hasError -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = contentAlpha)
        state.isReferenceNote -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
    }

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

    Surface(
        modifier = modifier,
        color = backgroundColor
    ) {
        Column(modifier = Modifier.padding(top=4.dp)) {
            if (state.isReferenceNote) {
                Text(stringResource(
                    id = R.string.default_reference_note),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }
        Row(
            modifier = Modifier
                .padding(bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${lineNumber+1}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .width(32.dp)
                    .padding(start = 16.dp),
                textAlign = TextAlign.Center,
                color = contentColor
                )
            TextButton(onClick = onNoteNameClicked) {
                Box(
                    modifier = Modifier.width(noteWidth),
                    contentAlignment = Alignment.Center
                ) {
                    state.note ?. let { note ->
                        Note(
                            note,
                            notePrintOptions = notePrintOptionsDefault,
                            withOctave = true,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                    }
                }
                Text(
                    "/",
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                Box(
                    modifier = Modifier.width(noteWidth),
                    contentAlignment = Alignment.Center
                ) {
                    state.note?.let { note ->
                        Note(
                            note,
                            notePrintOptions = notePrintOptionsOther,
                            withOctave = true,
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            TextField(
                value = state.centOrRatio ?: "",
                onValueChange = onValueChange,
                //trailingIcon = { Text(stringResource(id = R.string.cent_symbol))},
                label = {
                    Text(
                        if (isRatio)
                            stringResource(id = R.string.ratio)
                        else
                            stringResource(id = R.string.cent_str)
                    )
                },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                isError = state.hasError
            )
//            Text(
//                stringResource(R.string.cent_nosign, state.cent.roundToInt()),
//                modifier = Modifier.weight(1f),
//                textAlign = TextAlign.End
//            )
        }
            HorizontalDivider(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp))
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
                        isReferenceNote = index == 2,
                        isOctaveLine = index == notes.size - 1
                    )
                }

                TemperamentTableLine(
                    index,
                    state,
                    notePrintOptions = NotePrintOptions(notationType = NotationType.Solfege),
                    onValueChange = { state.changeCentOrRatio(it) }
                )
            }
        }
    }
}
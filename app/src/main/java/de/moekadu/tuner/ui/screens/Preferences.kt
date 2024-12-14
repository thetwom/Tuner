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
package de.moekadu.tuner.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.NightMode
import de.moekadu.tuner.ui.misc.rememberNumberFormatter
import de.moekadu.tuner.ui.notes.asAnnotatedString
import de.moekadu.tuner.ui.preferences.Section
import de.moekadu.tuner.ui.preferences.SimplePreference
import de.moekadu.tuner.ui.preferences.SliderPreference
import de.moekadu.tuner.ui.preferences.SwitchPreference
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.viewmodels.PreferencesViewModel
import kotlin.math.pow
import kotlin.math.roundToInt

private fun appearanceSummary(mode: NightMode, context: Context) = when (mode) {
    NightMode.Auto -> context.getString(R.string.system_appearance)
    NightMode.Off -> context.getString(R.string.light_appearance)
    NightMode.On -> context.getString(R.string.dark_appearance)
}

@Composable
fun Preferences(
    viewModel: PreferencesViewModel,
    modifier: Modifier = Modifier,
    onAppearanceClicked: () -> Unit = {},
    onReferenceFrequencyClicked: () -> Unit = {},
    onNotationClicked: () -> Unit = {},
    onTemperamentClicked: () -> Unit = {},
    onWindowingFunctionClicked: () -> Unit = {},
    onResetClicked: () -> Unit = {},
    onAboutClicked: () -> Unit = {}
) {
    val pref = viewModel.pref
    val context = LocalContext.current

    val notePrintOptions by pref.notePrintOptions.collectAsStateWithLifecycle()
    val musicalScale by viewModel.musicalScale.collectAsStateWithLifecycle()
    val decimalFormat = rememberNumberFormatter()

    LazyColumn(
        modifier = modifier
    ) {
        item {
            Section(
                title = stringResource(id = R.string.basic)
            )
        }
        item {
            val appearance by pref.appearance.collectAsStateWithLifecycle()
            SimplePreference(
                name = stringResource(id = R.string.appearance),
                supporting = appearanceSummary(appearance.mode, context),
                iconId = R.drawable.ic_appearance,
                modifier = Modifier.clickable { onAppearanceClicked() }
            )
        }
        item {
            val screenAlwaysOn by pref.screenAlwaysOn.collectAsStateWithLifecycle()
            SwitchPreference(
                name = stringResource(id = R.string.keep_screen_on),
                checked = screenAlwaysOn,
                onCheckChange = { pref.writeScreenAlwaysOn(it) },
                iconId = R.drawable.ic_screen_on
            )
        }
        item {
//            val referenceNote by pref.referenceFrequencyAsString.collectAsStateWithLifecycle()
            SimplePreference(
                name = stringResource(id = R.string.reference_frequency),
                supporting = {
                    val textStyle = LocalTextStyle.current
                    val resources = LocalContext.current.resources
                    val frequencyAsString = decimalFormat.format(musicalScale.referenceFrequency)
                    val summary = remember(musicalScale, notePrintOptions, textStyle, resources) {
                        buildAnnotatedString {
                            append(musicalScale.referenceNote.asAnnotatedString(
                                notePrintOptions,
                                textStyle.fontSize,
                                textStyle.fontWeight,
                                withOctave = true,
                                resources = resources
                            ))
                            append(" = ")
                            append(resources.getString(R.string.hertz_str, frequencyAsString))
                        }
                    }
                    Text(summary)
                },
                iconId = R.drawable.ic_frequency_a,
                modifier = Modifier.clickable { onReferenceFrequencyClicked() }
            )
        }
        item {
            SimplePreference(
                name = stringResource(id = R.string.temperament),
                supporting = {
                    val resources = LocalContext.current.resources
                    val textStyle = LocalTextStyle.current
                    val summary = remember(musicalScale, resources, textStyle) {
                        buildAnnotatedString {
                            //append(resources.getString(getTuningNameResourceId(musicalScale.temperamentType)))
                            append(musicalScale.temperament.name.value(context))
                            append(resources.getString(R.string.comma_separator))
                            append(musicalScale.rootNote.asAnnotatedString(
                                notePrintOptions,
                                textStyle.fontSize,
                                textStyle.fontWeight,
                                withOctave = false,
                                resources = resources
                            ))
                        }
                    }
                    Text(summary)
                },
                iconId = R.drawable.ic_temperament,
                modifier = Modifier.clickable { onTemperamentClicked() }
            )
        }
        item {
            val toleranceInCents by pref.toleranceInCents.collectAsStateWithLifecycle()

            SliderPreference(
                name = stringResource(id = R.string.tolerance_in_cents),
                supporting = stringResource(R.string.tolerance_summary, toleranceInCents),
                value = toleranceInCents.toFloat(),
                valueRange = 1f..20f,
                steps = 18,
                onValueChange = { pref.writeToleranceInCents(it.roundToInt()) },
                iconId = R.drawable.ic_tolerance
            )
        }
        item {
            SwitchPreference(
                name = stringResource(id = R.string.prefer_flat),
                checked = notePrintOptions.useEnharmonic,
                onCheckChange = {
                    val newNotePrintOptions = notePrintOptions.copy(useEnharmonic = it)
                    pref.writeNotePrintOptions(newNotePrintOptions)
                },
                iconId = R.drawable.ic_prefer_flat
            )
        }
        item {
            SimplePreference(
                name = stringResource(id = R.string.notation),
                supporting = stringResource(id = notePrintOptions.notationType.stringResourceId),
                iconId = R.drawable.ic_solfege,
                modifier = Modifier.clickable { onNotationClicked() }
            )
        }
        item {
            val sensitivity by pref.sensitivity.collectAsStateWithLifecycle()
            SliderPreference(
                name = stringResource(id = R.string.sensitivity),
                supporting = "$sensitivity",
                value = sensitivity.toFloat(),
                valueRange = 0f..100f,
                steps = 99,
                onValueChange = { pref.writeSensitivity(it.roundToInt()) },
                iconId = R.drawable.ic_harmonic_energy
            )
        }
        item {
            HorizontalDivider()
        }
        item {
            Section(title = stringResource(id = R.string.expert))
        }
        item {
            val scientificMode by pref.scientificMode.collectAsStateWithLifecycle()
            SwitchPreference(
                name = stringResource(id = R.string.scientific_mode),
                checked = scientificMode,
                onCheckChange = { pref.writeScientificMode(it) },
                iconId = R.drawable.ic_baseline_developer_board
            )
        }
        item {
            val numMovingAverage by pref.numMovingAverage.collectAsStateWithLifecycle()
            SliderPreference(
                name = stringResource(id = R.string.num_moving_average),
                supporting = LocalContext.current.resources.getQuantityString(
                    R.plurals.num_moving_average_summary,
                    numMovingAverage,
                    numMovingAverage
                ),
                value = numMovingAverage.toFloat(),
                valueRange = 1f..15f,
                steps = 13,
                onValueChange = { pref.writeNumMovingAverage(it.roundToInt()) },
                iconId = R.drawable.ic_moving_average
            )
        }
        item {
            val windowSizeExponent by pref.windowSizeExponent.collectAsStateWithLifecycle()
            val windowSize = 2f.pow(windowSizeExponent).roundToInt()
            val resources = LocalContext.current.resources
            val summary = remember(windowSize, resources) {
                "$windowSize " + resources.getString(R.string.samples) +
                        " (" + resources.getString(R.string.minimum_frequency) +
                        resources.getString(R.string.hertz,
                            2 * pref.sampleRate / windowSize.toFloat()
                        ) + ")"
            }
            SliderPreference(
                name = stringResource(id = R.string.window_size),
                supporting = summary,
                value = windowSizeExponent.toFloat(),
                valueRange = 7f..15f,
                steps = 7,
                onValueChange = { pref.writeWindowSize(it.roundToInt()) },
                iconId = R.drawable.ic_window_size
            )
        }
        item {
            val windowingFunction by pref.windowing.collectAsStateWithLifecycle()
            SimplePreference(
                name = stringResource(id = R.string.windowing_function),
                supporting = stringResource(id = windowingFunction.stringResourceId),
                iconId = R.drawable.ic_window_function,
                modifier = Modifier.clickable { onWindowingFunctionClicked() }
            )
        }
        item {
            val overlap by pref.overlap.collectAsStateWithLifecycle()
            SliderPreference(
                name = stringResource(id = R.string.overlap),
                value = 100 * overlap,
                supporting = stringResource(id = R.string.percent, (100 * overlap).roundToInt()),
                valueRange = 0f..80f,
                steps = 15,
                onValueChange = { pref.writeOverlap(it.roundToInt()) },
                iconId = R.drawable.ic_window_overlap
            )
        }
        item {
            val pitchHistoryDuration by pref.pitchHistoryDuration.collectAsStateWithLifecycle()
            SliderPreference(
                name = stringResource(id = R.string.pitch_history_duration),
                value = pitchHistoryDuration,
                supporting = stringResource(id = R.string.seconds, pitchHistoryDuration),
                valueRange = 0.25f..10f,
                steps = 38, // maybe better have progressive stps?
                onValueChange = { pref.writePitchHistoryDuration(it) },
                iconId = R.drawable.ic_duration
            )
        }
        item {
            val pitchHistoryNumFaultyValues by pref.pitchHistoryNumFaultyValues.collectAsStateWithLifecycle()
            val resources = LocalContext.current.resources
            val summary = remember(resources, pitchHistoryNumFaultyValues) {
                resources.getQuantityString(
                    R.plurals.pitch_history_num_faulty_values_summary,
                    pitchHistoryNumFaultyValues,
                    pitchHistoryNumFaultyValues
                )
            }
            SliderPreference(
                name = stringResource(id = R.string.pitch_history_num_faulty_values),
                value = pitchHistoryNumFaultyValues.toFloat(),
                supporting = summary,
                valueRange = 1f..12f,
                steps = 10,
                onValueChange = { pref.writePitchHistoryNumFaultyValues(it.roundToInt()) },
                iconId = R.drawable.ic_jump
            )
        }
        item {
            val duration by pref.waveWriterDurationInSeconds.collectAsStateWithLifecycle()
            SliderPreference(
                name = stringResource(id = R.string.capture),
                value = duration.toFloat(),
                supporting = if (duration == 0)
                    stringResource(R.string.no_capture_duration)
                else
                    stringResource(R.string.capture_duration, duration),
                valueRange = 0f..5f,
                steps = 4,
                onValueChange = { pref.writeWaveWriterDurationInSeconds(it.roundToInt()) },
                iconId = R.drawable.ic_mic
            )
        }
        item {
            HorizontalDivider()
        }
        item {
            Section(title = stringResource(id = R.string.others))
        }
        item {
            SimplePreference(
                name = stringResource(id = R.string.reset_all_settings),
                iconId = R.drawable.ic_reset,
                modifier = Modifier.clickable { onResetClicked() }
            )
        }
        item {
            SimplePreference(
                name = stringResource(id = R.string.about),
                iconId = R.drawable.ic_info,
                modifier = Modifier.clickable { onAboutClicked() }
            )
        }
    }
}

@Preview(widthDp = 400, heightDp = 800, showBackground = true)
@Composable
private fun PreferencesPreview() {
    TunerTheme {
        Preferences(
            viewModel = hiltViewModel(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
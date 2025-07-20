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
package de.moekadu.tuner.ui.preferences

import android.Manifest
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import de.moekadu.tuner.R
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.ui.misc.rememberNumberFormatter
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.NoteSelector
import de.moekadu.tuner.ui.theme.TunerTheme
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition

/** Parse a string, which is allowed to have white spaces.
 * @param string String to be parsed.
 * @return String as float of null if failed.
 */
private fun DecimalFormat.toFloatOrNull(string: String): Float? {
    return try {
        val trimmed = string.trim()
        val position = ParsePosition(0)
        val result = this.parse(trimmed, position)?.toFloat()
        if (position.index == trimmed.length)
            result
        else
            null
    } catch (ex: Exception) {
        null
    }
}

/** Interface for frequency detector as used by the RereferenceNoteDialog. */
interface ReferenceNoteDialogFrequencyDetector {
    /** The currently detected frequency or 0f if no frequency is detected yet. */
    val detectedFrequency: FloatState
    /** Start detecting frequencies. */
    fun startFrequencyDetection()
    /** Pause detecting frequencies. */
    fun stopFrequencyDetection()
}

/** Dialog for setting reference notes.
 * @param initialState Musical scale from where we take the initial state when opening the dialog.
 * @param onReferenceNoteChange Callback when the reference note is changed. This is called, when
 *   "confirm" is clicked.
 * @param notePrintOptions How to print the notes.
 * @param frequencyDetector Frequency detector which is used, when the reference frequency should
 *   be detected.
 * @param modifier Modifier.
 * @param warning A warning message which is printed on the dialog or null if no warning message
 *   should be printed. This is e.g. meant if a note scale is changed and we need to reset the
 *   reference note with a valid note, here we can show a message, that we need to reset the note.
 * @param onDismiss Callback when "dismiss" is clicked.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ReferenceNoteDialog(
    initialState: MusicalScale2,
    onReferenceNoteChange: (modifiedState: MusicalScale2) -> Unit,
    notePrintOptions: NotePrintOptions,
    frequencyDetector: ReferenceNoteDialogFrequencyDetector,
    modifier: Modifier = Modifier,
    warning: String? = null,
    onDismiss: () -> Unit = {}
) {
    var selectedNoteIndex by rememberSaveable { mutableIntStateOf(
        initialState.getNoteIndex2(initialState.referenceNote) - initialState.noteIndexBegin
    ) }
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocalConfiguration.current.locales[0]
    } else {
        LocalConfiguration.current.locale
    }
    val decimalFormat = rememberNumberFormatter()
    var frequencyAsString by rememberSaveable {
        mutableStateOf(decimalFormat.format(initialState.referenceFrequency))
    }
    val numberFormat = remember(locale) {
        NumberFormat.getNumberInstance(locale) as DecimalFormat
    }
    val validFrequency by remember { derivedStateOf {
        numberFormat.toFloatOrNull(frequencyAsString) != null
    }}
    var frequencyDetectorStarted by rememberSaveable { mutableStateOf(false) }
    val permission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)
    val permissionGranted by remember { derivedStateOf { permission.status.isGranted }}

    LifecycleResumeEffect(permissionGranted) {
        if (frequencyDetectorStarted && permissionGranted)
           frequencyDetector.startFrequencyDetection()
        onPauseOrDispose { frequencyDetector.stopFrequencyDetection() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val note = initialState.getNote(selectedNoteIndex + initialState.noteIndexBegin)
                    onReferenceNoteChange(
                        initialState.copy(
                            _referenceNote = note,
                            referenceFrequency = numberFormat.toFloatOrNull(frequencyAsString)
                                ?: initialState.referenceFrequency
                        )
                    )
                },
                enabled = validFrequency
            ) {
                Text(stringResource(id = R.string.done))
            }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.abort))
            }
        },
        icon = {
            Icon(
                ImageVector.vectorResource(id = R.drawable.ic_frequency_a),
                contentDescription = null
            )
        },
        title = {
            Text(stringResource(id = R.string.reference_frequency))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                warning?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
                NoteSelector(
                    selectedIndex = selectedNoteIndex,
                    musicalScale = initialState,
                    notePrintOptions = notePrintOptions,
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                    onIndexChanged = { selectedNoteIndex = it }
                )
                Spacer(
                    modifier = Modifier.height(8.dp)
                )
                TextField(
                    value = frequencyAsString,
                    onValueChange = { frequencyAsString = it },
                    label = { Text(stringResource(id = R.string.frequency))},
                    suffix = { Text(stringResource(id = R.string.hertz_str, ""))},
                    isError = !validFrequency,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (permissionGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (frequencyDetectorStarted) {
                        Row {
                            OutlinedButton(
                                onClick = {
                                    frequencyAsString = "%.2f".format(locale, frequencyDetector.detectedFrequency.floatValue)
                                },
                                modifier = Modifier.weight(1f),
                                enabled = frequencyDetector.detectedFrequency.floatValue != 0f
                            ) {
                                Text(stringResource(
                                    R.string.hertz_str,
                                    "%.2f".format(locale, frequencyDetector.detectedFrequency.floatValue))
                                )
                            }
                            IconButton(
                                onClick = {
                                    frequencyDetectorStarted = false
                                    frequencyDetector.stopFrequencyDetection()
                                }
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "stop frequency detection")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (!frequencyDetectorStarted) {
                                    frequencyDetectorStarted = true
                                    frequencyDetector.startFrequencyDetection()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.detect_frequency))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val note = initialState.temperament.noteNames(initialState.rootNote).defaultReferenceNote
                        selectedNoteIndex = initialState.getNoteIndex2(note) - initialState.noteIndexBegin
                        frequencyAsString = decimalFormat.format(PreferenceResources.ReferenceFrequencyDefault)
                    },
                    modifier = Modifier.fillMaxWidth()
                    ) {
                    Text(stringResource(id = R.string.set_default))
                }
            }
        }
    )
}

private class TestReferenceNoteDialogFrequencyDetector : ReferenceNoteDialogFrequencyDetector {
    private val _detectedFrequency = mutableFloatStateOf(443.0f)
    override val detectedFrequency: FloatState get() = _detectedFrequency
    override fun startFrequencyDetection() {}
    override fun stopFrequencyDetection() {}
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun AppearanceDialogTest() {
    TunerTheme {
        val state = remember { MusicalScale2.createTestEdo12() }
        val notePrintOptions = remember { NotePrintOptions() }
        ReferenceNoteDialog(
            state,
            notePrintOptions = notePrintOptions,
            frequencyDetector = TestReferenceNoteDialogFrequencyDetector(),
            onReferenceNoteChange = { }
        )
    }
}

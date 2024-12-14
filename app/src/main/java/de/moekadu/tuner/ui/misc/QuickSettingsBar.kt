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
package de.moekadu.tuner.ui.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.ui.notes.Note
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun QuickSettingsBar(
    musicalScale: MusicalScale,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    onSharpFlatClicked: () -> Unit = {},
    onTemperamentClicked: () -> Unit = {},
    onReferenceNoteClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val decimalFormat = rememberNumberFormatter()

    Row(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onReferenceNoteClicked,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Note(
                    musicalNote = musicalScale.referenceNote,
                    notePrintOptions = notePrintOptions,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(id = R.string.hertz_str, decimalFormat.format(musicalScale.referenceFrequency)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            onClick = onTemperamentClicked,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val abbreviation = musicalScale.temperament.abbreviation.value(context).let { abbr ->
                    if (abbr == "")
                        musicalScale.temperament.name.value(context).let { nme ->
                            if (nme == "") "-" else nme
                        }
                    else
                        abbr
                }

                Text(
                    abbreviation,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            onClick = onSharpFlatClicked,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ImageVector.vectorResource(
                        if (notePrintOptions.useEnharmonic)
                            R.drawable.ic_prefer_flat_isflat
                        else
                            R.drawable.ic_prefer_flat_issharp
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

}

@Preview(widthDp = 300, heightDp = 48)
@Composable
private fun QuickSettingsBarPreview() {
    TunerTheme {
        var notePrintOptions by remember { mutableStateOf(NotePrintOptions()) }
        val musicalScale = remember { MusicalScaleFactory.createTestEdo12() }

        QuickSettingsBar(
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            onSharpFlatClicked = {
                notePrintOptions = notePrintOptions.copy(
                    useEnharmonic = !notePrintOptions.useEnharmonic
                )
            }
        )
    }
}
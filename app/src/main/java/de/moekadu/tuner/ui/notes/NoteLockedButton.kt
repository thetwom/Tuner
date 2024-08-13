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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.times
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.ui.theme.TunerTheme
import de.moekadu.tuner.ui.theme.tunerTypography

@Composable
fun NoteLockedButton(
    note: MusicalNote,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    fontSize: TextUnit = MaterialTheme.tunerTypography.plotLarge.fontSize
) {
    val iconSize = with(LocalDensity.current) {
        0.8f * fontSize.toDp()
    }
    val resources = LocalContext.current.resources
    val buttonText = remember(note, fontSize, resources, notePrintOptions) {
        note.asAnnotatedString(
            notePrintOptions = notePrintOptions,
            fontSize = fontSize,
            fontWeight = null,
            withOctave = true,
            resources = resources
        )
    }
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "locked",
                modifier = Modifier.size(iconSize)
            )
            Text(
                buttonText,
                textAlign = TextAlign.Center
            )
        }
        Icon(Icons.Default.Clear, contentDescription = "clear")
    }
}

@Preview(widthDp = 300, heightDp = 200, showBackground = true)
@Composable
private fun NoteLockedButtonPreview() {
    TunerTheme {
        Column {
            NoteLockedButton(
                note = MusicalNote(BaseNote.A, NoteModifier.Flat),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
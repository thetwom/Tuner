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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.temperaments.getTuningNameAbbrResourceId
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
                Text(
                    stringResource(id = getTuningNameAbbrResourceId(musicalScale.temperamentType)),
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
                        if (notePrintOptions.sharpFlatPreference == NotePrintOptions.SharpFlatPreference.Flat)
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
        var notePrintOptions by remember {
            mutableStateOf(NotePrintOptions())
        }

        val musicalScale by remember { mutableStateOf(
            MusicalScaleFactory.create(
                TemperamentType.EDO12,
                referenceFrequency = 432.1554f
            )
        ) }

        QuickSettingsBar(
            musicalScale = musicalScale,
            notePrintOptions = notePrintOptions,
            onSharpFlatClicked = {
                notePrintOptions = notePrintOptions.copy(
                    sharpFlatPreference = if (notePrintOptions.sharpFlatPreference == NotePrintOptions.SharpFlatPreference.Flat)
                            NotePrintOptions.SharpFlatPreference.Sharp
                        else
                            NotePrintOptions.SharpFlatPreference.Flat
                )
            }
        )
    }
}
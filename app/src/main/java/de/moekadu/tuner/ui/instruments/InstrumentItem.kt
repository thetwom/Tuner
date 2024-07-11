package de.moekadu.tuner.ui.instruments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.temperaments.BaseNote
import de.moekadu.tuner.temperaments.MusicalNote
import de.moekadu.tuner.temperaments.NoteModifier
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun InstrumentItem(
    instrument: Instrument,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions()
) {
    val context = LocalContext.current
   ListItem(
       headlineContent = {
           Text(
               instrument.getNameString2(context)
           )
       },
       leadingContent = {
           Icon(
               ImageVector.vectorResource(id = instrument.iconResource),
               modifier = Modifier.size(40.dp),
               contentDescription = null
           )
       },
       supportingContent = {
           val style = LocalTextStyle.current
           val stringsString = remember(context, style, instrument) {
               instrument.getStringsString2(
                   context = context,
                   notePrintOptions = notePrintOptions,
                   style.fontSize,
                   style.fontWeight
                   
               )
           }
           Text(stringsString)
       },
       modifier = modifier
   )
}

enum class InstrumentItemTask {
    Copy,
    Edit,
    Delete
}


@Composable
fun InstrumentItem2(
    //instrument: InstrumentInList,
    instrument: Instrument,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    onOptionsClicked: (instrument: Instrument, task: InstrumentItemTask) -> Unit = {_, _ ->},
    isActive: Boolean = false,
    isSelected: Boolean = false,
    readOnly: Boolean = false, // disable delete/edit options
    isCopyable: Boolean = true // disable copy-option
) {
    val context = LocalContext.current
    val variantColor = if (isActive)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    var menuExpanded by remember{ mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(vertical = 8.dp)
        ) {
            Spacer(Modifier.width(16.dp))
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(horizontal = 4.dp),
                    tint = variantColor
                )
            } else {
                Icon(
                    ImageVector.vectorResource(id = instrument.iconResource),
                    modifier = Modifier.size(40.dp),
                    contentDescription = null,

                    tint = variantColor
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val supportingStyle = MaterialTheme.typography.bodyMedium
                val stringsString = remember(context, supportingStyle, instrument) {
                    instrument.getStringsString2(
                        context = context,
                        notePrintOptions = notePrintOptions,
                        supportingStyle.fontSize,
                        supportingStyle.fontWeight
                    )
                }
                Text(
                    instrument.getNameString2(context),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringsString,
                    color = variantColor,
                    style = supportingStyle
                )
            }
            Box {
                if (readOnly && isCopyable) {
                    IconButton(
                        onClick = { onOptionsClicked(instrument, InstrumentItemTask.Copy) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_copy),
                            contentDescription = "copy",
                            tint = variantColor
                        )
                    }
                } else if (!readOnly) {

                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = variantColor
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.edit_instrument)) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "edit") },
                            onClick = {
                                menuExpanded = false
                                onOptionsClicked(instrument, InstrumentItemTask.Edit)
                            }
                        )
                        if (isCopyable) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.copy_instrument)) },
                                leadingIcon = {
                                    Icon(
                                        ImageVector.vectorResource(id = R.drawable.ic_copy),
                                        contentDescription = "copy"
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onOptionsClicked(instrument, InstrumentItemTask.Copy)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.delete_instrument)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "delete"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOptionsClicked(instrument, InstrumentItemTask.Delete)
                            }
                        )
                    }
                }
            }
        }
    }

}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun InstrumentItemPreview() {
    TunerTheme {
        val instrument = remember {
            Instrument(
                name = null,
                nameResource = R.string.guitar_eadgbe,
                strings = arrayOf(
                    MusicalNote(BaseNote.A, NoteModifier.None, octave = 4),
                    MusicalNote(BaseNote.G, NoteModifier.None, octave = 4),
                    MusicalNote(BaseNote.D, NoteModifier.Sharp, octave = 3),
                    MusicalNote(BaseNote.E, NoteModifier.None, octave = 2),
                ),
                iconResource = R.drawable.ic_guitar,
                1L,
                isChromatic = false
            )
        }
        val instrument2 = remember {
            Instrument(
                name = null,
                nameResource = R.string.chromatic,
                strings = arrayOf(),
                iconResource = R.drawable.ic_piano,
                stableId = 2L,
                isChromatic = true
            )
        }
        Column {
            InstrumentItem(
                instrument = instrument
            )
            InstrumentItem(
                instrument = instrument2
            )
            InstrumentItem2(
                instrument = instrument
            )
            InstrumentItem2(
                instrument = instrument,
                isSelected = true
            )
            InstrumentItem2(
                instrument = instrument,
                isActive = true,
                readOnly = true
            )
        }
    }
}

package de.moekadu.tuner.ui.temperaments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.temperaments2.MusicalScale2
import de.moekadu.tuner.temperaments2.MusicalScale2Factory
import de.moekadu.tuner.temperaments2.NoteNames
import de.moekadu.tuner.temperaments2.StretchTuning
import de.moekadu.tuner.temperaments2.Temperament
import de.moekadu.tuner.temperaments2.getSuitableNoteNames
import de.moekadu.tuner.ui.notes.CentAndRatioTable
import de.moekadu.tuner.ui.notes.CentTable
import de.moekadu.tuner.ui.notes.CircleOfFifthTable
import de.moekadu.tuner.ui.notes.Note
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.notes.RatioTable
import de.moekadu.tuner.ui.theme.TunerTheme

enum class ActiveTemperamentDetailChoice{
    Off,
    Cents,
    Ratios,
    CircleOfFifth
}

@Composable
fun ActiveTemperament(
    temperament: Temperament,
    noteNames: NoteNames,
    rootNoteIndex: Int,
    detailChoice: ActiveTemperamentDetailChoice,
    onChooseDetail: (ActiveTemperamentDetailChoice) -> Unit,
    notePrintOptions: NotePrintOptions,
    modifier: Modifier = Modifier,
    onResetClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
//            Text(
//                stringResource(id = R.string.current_selection),
//                modifier = Modifier.fillMaxWidth().padding(top=4.dp),
//                textAlign = TextAlign.Center,
//                style = MaterialTheme.typography.labelSmall
//            )
            Row(
                verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.onPrimary),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Note(
                            noteNames[rootNoteIndex % noteNames.size],
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        temperament.name.value(context),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        temperament.description.value(context),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = !menuExpanded },
                        modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_info),
                            contentDescription = "details"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.hide_details)) },
                            leadingIcon = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_visibility_off),
                                    contentDescription = "hide details"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onChooseDetail(ActiveTemperamentDetailChoice.Off)
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.list_of_cents)) },
                            leadingIcon = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_cent),
                                    contentDescription = "cents"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onChooseDetail(ActiveTemperamentDetailChoice.Cents)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.list_of_ratios)) },
                            leadingIcon = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_ratio),
                                    contentDescription = "ratios"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onChooseDetail(ActiveTemperamentDetailChoice.Ratios)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.circle_of_fifths)) },
                            leadingIcon = {
                                Icon(
                                    ImageVector.vectorResource(id = R.drawable.ic_temperament),
                                    contentDescription = "circle of fifth"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onChooseDetail(ActiveTemperamentDetailChoice.CircleOfFifth)
                            }
                        )
                    }
                }
            }

            TextButton(
                onClick = onResetClicked,
                colors = ButtonDefaults.buttonColors().copy(contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) //, vertical = 8.dp)
            ) {
                Text(
                    stringResource(id = R.string.set_default),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            //Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = (detailChoice != ActiveTemperamentDetailChoice.Off)) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            AnimatedVisibility(visible = (detailChoice == ActiveTemperamentDetailChoice.Cents)) {
                Column {
                    Text(
                        stringResource(id = R.string.list_of_cents),
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )


                    CentTable(
                        temperament,
                        noteNames,
                        rootNoteIndex = rootNoteIndex,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            AnimatedVisibility(visible = (detailChoice == ActiveTemperamentDetailChoice.Ratios)) {
                Column {
                    Text(
                        stringResource(id = R.string.list_of_ratios),
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )

                    RatioTable(
                        temperament,
                        noteNames,
                        rootNoteIndex = rootNoteIndex,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            AnimatedVisibility(visible = (detailChoice == ActiveTemperamentDetailChoice.CircleOfFifth)) {
                Column(
                    Modifier
                        .height(80.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        stringResource(id = R.string.circle_of_fifths),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )

                    CircleOfFifthTable(
                        temperament,
                        noteNames,
                        rootNoteIndex = rootNoteIndex,
                        notePrintOptions = notePrintOptions,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(id = R.string.pythagorean_comma_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}




@Preview(widthDp = 250, heightDp = 400, showBackground = true)
@Composable
private fun ActiveTemperamentPreview() {
    TunerTheme {
        val temperament = remember {
            Temperament.create(
                StringOrResId("Test"),
                StringOrResId("T"),
                StringOrResId("Test description"),
                12,
                -1L
            )
        }
        val noteNames = remember {
            getSuitableNoteNames(temperament.numberOfNotesPerOctave)!!
        }

        var detailChoice by remember { mutableStateOf(ActiveTemperamentDetailChoice.Off) }

        Column {

            ActiveTemperament(
                temperament,
                noteNames,
                rootNoteIndex = 1,
                detailChoice = detailChoice,
                onChooseDetail = { detailChoice = it },
                notePrintOptions = NotePrintOptions()
            )
        }
    }
}
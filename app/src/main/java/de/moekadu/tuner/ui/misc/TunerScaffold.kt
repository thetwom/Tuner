package de.moekadu.tuner.ui.misc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.MusicalScale
import de.moekadu.tuner.temperaments.MusicalScaleFactory
import de.moekadu.tuner.temperaments.TemperamentType
import de.moekadu.tuner.ui.notes.NotePrintOptions
import de.moekadu.tuner.ui.theme.TunerTheme

private enum class NavigationIconState{
    Off, Arrow, Clear
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScaffold(
    modifier: Modifier = Modifier,
    canNavigateUp: Boolean = true,
    onNavigateUpClicked: () -> Unit = {},
    showPreferenceButton: Boolean = true,
    onPreferenceButtonClicked: () -> Unit = {},
    actionModeActive: Boolean = false,
    actionModeTitle: String = "",
    actionModeTools: @Composable (RowScope.() -> Unit) = {},
    onActionModeFinishedClicked: () -> Unit = {},
    showBottomBar: Boolean = true,
    onSharpFlatClicked: () -> Unit = {},
    onTemperamentClicked: () -> Unit = {},
    onReferenceNoteClicked: () -> Unit = {},
    musicalScale: MusicalScale = MusicalScaleFactory.create(TemperamentType.EDO12),
    notePrintOptions: NotePrintOptions = NotePrintOptions(),
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (actionModeActive)
                        Text(actionModeTitle)
                    else
                        Text(stringResource(R.string.app_name))
                },
                navigationIcon = {
                    val state = when {
                        actionModeActive -> NavigationIconState.Clear
                        canNavigateUp -> NavigationIconState.Arrow
                        else -> NavigationIconState.Off
                    }
                    when (state) {
                        NavigationIconState.Clear -> {
                            IconButton(onClick = onActionModeFinishedClicked) {
                                Icon(Icons.Default.Close, "close")
                            }
                        }
                        NavigationIconState.Arrow -> {
                            IconButton(onClick = onNavigateUpClicked) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "back")
                            }

                        }
                        NavigationIconState.Off -> {}}
                },
                actions = {
                    if (actionModeActive) {
                        actionModeTools()
                    } else if (showPreferenceButton) {
                        IconButton(onClick = onPreferenceButtonClicked) {
                            Icon(Icons.Filled.Settings, "settings")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                QuickSettingsBar(
                    musicalScale = musicalScale,
                    notePrintOptions = notePrintOptions,
                    onSharpFlatClicked = onSharpFlatClicked,
                    onTemperamentClicked = onTemperamentClicked,
                    onReferenceNoteClicked = onReferenceNoteClicked
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun TunerScaffoldPreview() {
    TunerTheme {
        var actionMode by remember { mutableStateOf(false) }
        var showBottomBar by remember { mutableStateOf(true) }

        TunerScaffold(
            actionModeActive = actionMode,
            actionModeTools = {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Edit, contentDescription = "edit")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Build, contentDescription = "build")
                }
            },
            actionModeTitle = "Edit",
            onActionModeFinishedClicked = { actionMode = false },
            showBottomBar = showBottomBar
        ) {
            Column(modifier = Modifier.padding(it)) {
                Button(onClick = { actionMode = !actionMode }) {
                    Text(
                        "EnableAction",
                    )
                }
                Button(onClick = { showBottomBar = !showBottomBar }) {
                    Text(
                        "Show bottom bar",
                    )
                }
            }
        }
    }
}
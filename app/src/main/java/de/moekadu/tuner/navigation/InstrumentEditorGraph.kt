package de.moekadu.tuner.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import de.moekadu.tuner.preferences.PreferenceResources2
import de.moekadu.tuner.ui.instruments.InstrumentIconPicker
import de.moekadu.tuner.ui.screens.InstrumentEditor
import de.moekadu.tuner.viewmodels.InstrumentEditorViewModel2
import de.moekadu.tuner.viewmodels.InstrumentViewModel2
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

fun NavGraphBuilder.instrumentEditorGraph(
    controller: NavController,
    canNavigateUp: Boolean,
    onNavigateUpClicked: () -> Unit,
    preferences: PreferenceResources2,
    scope: CoroutineScope
) {
    navigation<InstrumentEditorGraphRoute>(
        startDestination =  InstrumentEditorRoute
    ) {
        composable<InstrumentEditorRoute> {
            val parentEntry = remember(it) { controller.getBackStackEntry(InstrumentEditorGraphRoute) }
            val viewModel: InstrumentEditorViewModel2 = hiltViewModel(parentEntry)
            InstrumentEditor(
                state = viewModel,
                onIconButtonClicked = { controller.navigate(IconPickerRoute) }
            )
        }
        dialog<IconPickerRoute> {
            val parentEntry = remember(it) { controller.getBackStackEntry(InstrumentEditorGraphRoute) }
            val viewModel: InstrumentEditorViewModel2 = hiltViewModel(parentEntry)
            InstrumentIconPicker(
                onDismiss = { controller.navigateUp() },
                onIconSelected = {
                    viewModel.setIcon(it)
                    controller.navigateUp()
                }
            )
        }
    }
}

@Serializable
data object InstrumentEditorGraphRoute
@Serializable
data object InstrumentEditorRoute
@Serializable
data object IconPickerRoute
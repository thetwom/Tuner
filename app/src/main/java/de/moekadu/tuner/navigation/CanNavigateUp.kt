package de.moekadu.tuner.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController

@Composable
fun NavController.rememberCanNavigateUp(): Boolean {
    var canNavigateUp by remember { mutableStateOf(false) }
    val controller = this
    DisposableEffect(controller) {
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            canNavigateUp = controller.previousBackStackEntry != null
        }
        controller.addOnDestinationChangedListener(listener)
        onDispose {
            controller.removeOnDestinationChangedListener(listener)
        }
    }
    return canNavigateUp
}
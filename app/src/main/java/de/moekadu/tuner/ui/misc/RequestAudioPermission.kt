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

import android.Manifest
import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import de.moekadu.tuner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
private fun CoroutineScope.launchSnackbar(
    context: Context,
    snackbarHostState: SnackbarHostState,
    permission: PermissionState
) {
    launch {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.audio_record_permission_rationale),
            actionLabel = context.getString(R.string.settings),
            withDismissAction = false
        )
        when (result) {
            SnackbarResult.Dismissed -> {}
            SnackbarResult.ActionPerformed -> {
                permission.launchPermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberTunerAudioPermission(snackbarHostState: SnackbarHostState): Boolean {
    val context = LocalContext.current

    val reopenSnackbarChannel = remember { Channel<Boolean>(Channel.CONFLATED) }
    val permission = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO) {
        if (!it)
            reopenSnackbarChannel.trySend(false)
    }
    val permissionGranted by remember { derivedStateOf { permission.status.isGranted }}
//    Log.v("Tuner", "MainGraph: 1: permissions_granted = ${permission.status.isGranted}, rational = ${permission.status.shouldShowRationale}")

    // TODO: relaunching ssems to fail ...
    LaunchedEffect(permission.status, reopenSnackbarChannel) {
        if (!permission.status.isGranted) {
            if (permission.status.shouldShowRationale)
                launchSnackbar(context, snackbarHostState, permission)
            else
                permission.launchPermissionRequest()

            for (reopen in reopenSnackbarChannel)
                launchSnackbar(context, snackbarHostState, permission)
        }
    }
    return permissionGranted
}

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
package de.moekadu.tuner.viewmodels

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.preferences.PreferenceResources
import de.moekadu.tuner.tuner.SimpleFrequencyDetector
import de.moekadu.tuner.ui.preferences.ReferenceNoteDialogFrequencyDetector
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

/** We provide the frequency detector via a view model.
 * @note We provide this via a view model for simple injection of preferences.
 *   However, we might also be able to inject this directly in the composable?
 * @param pref Preferences from where we get the parameters for the frequency detector.
 * @param applicationScope Coroutine scope which is used for launching the frequency detector.
 */
@HiltViewModel
class ReferenceNoteDialogViewModel @Inject constructor (
    val pref: PreferenceResources,
    @ApplicationScope val applicationScope: CoroutineScope
) : ReferenceNoteDialogFrequencyDetector, ViewModel() {
    private var _detectedFrequency = mutableFloatStateOf(0f)
    /** The currently detected frequency or 0f if no frequency is detected yet.
     * @note Call startFrequencyDetection to actually start setting this value.
     */
    override val detectedFrequency: FloatState get() = _detectedFrequency

    /** The frequency detector. */
    private val frequencyDetector = SimpleFrequencyDetector(
        viewModelScope,
        onFrequencyAvailable = {
            if (it > 0f)
                _detectedFrequency.floatValue = it
        },
        pref
    )

    /** Start frequency detection. */
    override fun startFrequencyDetection() {
        frequencyDetector.connect()
    }
    /** Pause frequency detection. */
    override fun stopFrequencyDetection() {
        frequencyDetector.disconnect()
    }
}
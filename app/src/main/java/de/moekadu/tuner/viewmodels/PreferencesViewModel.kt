package de.moekadu.tuner.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.preferences.PreferenceResources2
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    val pref: PreferenceResources2
) : ViewModel() {

}
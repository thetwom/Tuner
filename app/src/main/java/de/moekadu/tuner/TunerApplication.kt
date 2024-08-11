package de.moekadu.tuner

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.preferences.PreferenceResources
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob

val Context.preferenceResources: PreferenceResources
    get() = (applicationContext as TunerApplication).preferenceResources

val Context.instrumentResources: InstrumentResources
    get() = (applicationContext as TunerApplication).instrumentResources

@HiltAndroidApp
class TunerApplication : Application() {
    // TODO: use hilt to generate the preferences
    //lateinit var sharedPreferences: SharedPreferences
    //lateinit var preferenceResources: PreferenceResources

    //lateinit var instrumentPreferences: SharedPreferences
    //lateinit var instrumentResources: InstrumentResources

}
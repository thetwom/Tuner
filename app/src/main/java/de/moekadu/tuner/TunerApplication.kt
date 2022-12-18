package de.moekadu.tuner

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import de.moekadu.tuner.instruments.InstrumentResources
import de.moekadu.tuner.preferences.PreferenceResources
import kotlinx.coroutines.MainScope

val Context.preferenceResources: PreferenceResources
    get() = (applicationContext as TunerApplication).preferenceResources

val Context.instrumentResources: InstrumentResources
    get() = (applicationContext as TunerApplication).instrumentResources

class TunerApplication : Application() {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var preferenceResources: PreferenceResources

    lateinit var instrumentPreferences: SharedPreferences
    lateinit var instrumentResources: InstrumentResources

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferenceResources = PreferenceResources(sharedPreferences, MainScope())
        instrumentPreferences = getSharedPreferences("instrument resources", MODE_PRIVATE)
        instrumentResources = InstrumentResources(instrumentPreferences) // TODO: migrate preference, by calling the migration funcion within the activity

        val appearance = preferenceResources.appearance.value
        if (appearance.mode != AppCompatDelegate.getDefaultNightMode())
            AppCompatDelegate.setDefaultNightMode(appearance.mode)

//        val useSystemColors = AppearancePreference.getUseSystemColorAccents(appearanceString)
//        if (useSystemColors)
//            DynamicColors.applyToActivitiesIfAvailable(this)

    }
}
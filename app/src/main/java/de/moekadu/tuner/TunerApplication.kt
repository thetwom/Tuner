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

val Context.preferenceResources: PreferenceResources
    get() = (applicationContext as TunerApplication).preferenceResources

val Context.instrumentResources: InstrumentResources
    get() = (applicationContext as TunerApplication).instrumentResources

@HiltAndroidApp
class TunerApplication : Application() {
    // TODO: use hilt to generate the preferences
    //lateinit var sharedPreferences: SharedPreferences
    lateinit var preferenceResources: PreferenceResources

    //lateinit var instrumentPreferences: SharedPreferences
    lateinit var instrumentResources: InstrumentResources

    override fun onCreate() {
        super.onCreate()
        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        //preferenceResources = PreferenceResources(this, sharedPreferences, MainScope())
        preferenceResources = PreferenceResources(this)
        //instrumentPreferences = getSharedPreferences("instrument resources", MODE_PRIVATE)
        //instrumentResources = InstrumentResources(instrumentPreferences)
        instrumentResources = InstrumentResources(this)

        val appearance = preferenceResources.appearance.value
        if (appearance.mode != AppCompatDelegate.getDefaultNightMode())
            AppCompatDelegate.setDefaultNightMode(appearance.mode)

//        val useSystemColors = AppearancePreference.getUseSystemColorAccents(appearanceString)
//        if (useSystemColors)
//            DynamicColors.applyToActivitiesIfAvailable(this)

    }
}
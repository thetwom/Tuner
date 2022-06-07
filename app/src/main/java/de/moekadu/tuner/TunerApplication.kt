package de.moekadu.tuner

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import de.moekadu.tuner.preferences.AppearancePreference

class TunerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val appearanceString = sharedPreferences.getString("appearance", "auto") ?: "auto"
        val nightMode = AppearancePreference.getUIModeFromValue(appearanceString)
        //val nightMode = nightModeStringToID(nightModeString)
        if (nightMode != AppCompatDelegate.getDefaultNightMode()) {
//            Log.v("Tuner", "TunerApplication.onCreate: setting night mode to $nightMode")
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

//        val useSystemColors = AppearancePreference.getUseSystemColorAccents(appearanceString)
//        if (useSystemColors)
//            DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
package de.moekadu.tuner

import android.app.Application
import com.google.android.material.color.DynamicColors

class TunerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

    }
}
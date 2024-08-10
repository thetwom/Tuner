package de.moekadu.tuner.instruments

import android.content.Context

fun InstrumentResources2.migratingFromV6(context: Context) {
    val resources = InstrumentResources(context)
    resources.customInstrumentsExpanded?.let {
        this.writeCustomInstrumentsExpanded(it)
    }
    resources.predefinedInstrumentsExpanded?.let {
        this.writePredefinedInstrumentsExpanded(it)
    }
    resources.customInstruments?.let {
        this.writeCustomInstruments(it)
    }
    resources.currentInstrument?.let {
        this.writeCurrentInstrument(it)
    }
}
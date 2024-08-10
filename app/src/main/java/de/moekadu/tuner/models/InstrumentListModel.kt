package de.moekadu.tuner.models

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentResources

class InstrumentListModel(
    val predefinedInstruments: List<Instrument>,
    val customInstruments: List<Instrument>,
    val predefinedInstrumentsExpanded: Boolean,
    val customInstrumentsExpanded: Boolean,
    val areSectionTitlesVisible: Boolean,
    val instrumentAndSection: InstrumentResources.InstrumentAndSection
) {
    companion object {
//        fun fromInstrumentResources(resources: InstrumentResources): InstrumentListModel {
//            return InstrumentListModel(
//                if (resources.predefinedInstrumentsExpanded.value)
//                    resources.predefinedInstruments
//                else
//                    ArrayList()
//                ,
//                if (resources.customInstrumentsExpanded.value)
//                    resources.customInstruments.value
//                else
//                    ArrayList()
//                ,
//                predefinedInstrumentsExpanded = resources.predefinedInstrumentsExpanded.value || resources.customInstruments.value.isEmpty(),
//                customInstrumentsExpanded = resources.customInstrumentsExpanded.value,
//                areSectionTitlesVisible = resources.customInstruments.value.isNotEmpty(),
//                instrumentAndSection = resources.instrument.value
//            )
//        }
    }
}

package de.moekadu.tuner.instruments

import de.moekadu.tuner.R

class InstrumentIcon(val resourceId: Int, val name: String)

val instrumentIcons = arrayOf(
    InstrumentIcon(R.drawable.ic_guitar, "guitar"),
    InstrumentIcon(R.drawable.ic_ukulele, "ukulele"),
    InstrumentIcon(R.drawable.ic_eguitar, "eguitar"),
    InstrumentIcon(R.drawable.ic_bass, "bass"),
    InstrumentIcon(R.drawable.ic_violin, "violin"),
    InstrumentIcon(R.drawable.ic_cello, "cello"),
    InstrumentIcon(R.drawable.ic_double_bass, "double bass"),
    InstrumentIcon(R.drawable.ic_trumpet, "trumpet"),
    InstrumentIcon(R.drawable.ic_saxophone, "saxophone"),
    InstrumentIcon(R.drawable.ic_flute, "flute"),
    InstrumentIcon(R.drawable.ic_harp, "harp"),
    InstrumentIcon(R.drawable.ic_tar, "tar"),
    InstrumentIcon(R.drawable.ic_zetar, "zetar"),
    InstrumentIcon(R.drawable.ic_kamancheh, "kamancheh"),
    InstrumentIcon(R.drawable.ic_piano, "piano")
)

fun instrumentIconId2Name(resourceId: Int): String {
    return (instrumentIcons.firstOrNull { it.resourceId == resourceId }?: instrumentIcons[0]).name
}

fun instrumentIconName2Id(name: String): Int {
    return (instrumentIcons.firstOrNull { it.name == name }?: instrumentIcons[0]).resourceId
}


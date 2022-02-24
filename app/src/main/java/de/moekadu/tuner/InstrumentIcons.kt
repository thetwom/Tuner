package de.moekadu.tuner

class InstrumentIcon(val resourceId: Int, val name: String)

val instrumentIcons = arrayOf(
    InstrumentIcon(R.drawable.ic_guitar, "guitar"),
    InstrumentIcon(R.drawable.ic_bass, "bass"),
    InstrumentIcon(R.drawable.ic_ukulele, "ukulele"),
    InstrumentIcon(R.drawable.ic_violin, "violin"),
    InstrumentIcon(R.drawable.ic_piano, "piano"),
)

fun instrumentIconId2Name(resourceId: Int): String {
    return (instrumentIcons.firstOrNull { it.resourceId == resourceId }?: instrumentIcons[0]).name
}

fun instrumentIconName2Id(name: String): Int {
    return (instrumentIcons.firstOrNull { it.name == name }?: instrumentIcons[0]).resourceId
}


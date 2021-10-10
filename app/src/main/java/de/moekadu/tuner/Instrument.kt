package de.moekadu.tuner

enum class InstrumentType {
    Piano, Guitar, Bass, Ukulele
}

data class Instrument(val name: CharSequence, val strings: IntArray, val type: InstrumentType, val id: Int) {
    val stringsSorted = strings.map { it.toFloat() }.toFloatArray().sortedArray()
}

val instrumentDatabase = createInstrumentDatabase()

private fun createInstrumentDatabase(): ArrayList<Instrument> {
    val instruments = ArrayList<Instrument>()
    instruments.add(
        Instrument(
            name = "Piano",
            strings = intArrayOf(),
            type = InstrumentType.Piano,
            id = instruments.size
        )
    )
    instruments.add(
        Instrument(
            name = "6-string guitar (E-A-D-G-B-E)",
            strings = intArrayOf(-29, -24, -19, -14, -10, -5),
            type = InstrumentType.Guitar,
            id = instruments.size
        )
    )
    instruments.add(
        Instrument(
            name = "4-string bass (E-A-D-G)",
            strings = intArrayOf(-41, -36, -31, -26),
            type = InstrumentType.Bass,
            id = instruments.size
        )
    )
    instruments.add(
        Instrument(
            name = "5-string bass (B-E-A-D-G)",
            strings = intArrayOf(-46, -41, -36, -31, -26),
            type = InstrumentType.Bass,
            id = instruments.size
        )
    )
    instruments.add(
        Instrument(
            name = "Ukulele (G-C-E-A)",
            strings = intArrayOf(-5, 0, 5, 10),
            type = InstrumentType.Ukulele,
            id = instruments.size
        )
    )
    return instruments
}

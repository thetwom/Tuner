package de.moekadu.tuner

class InstrumentDatabase {
    private val instruments = ArrayList<Instrument>()

    init {
        instruments.add(GuitarEADGHE())
        instruments.add(BassEADG())
        instruments.add(BassBEADG())
        instruments.add(UkuleleGCEA())
    }
}

enum class InstrumentType {
    Guitar, Bass, Ukulele
}

interface Instrument {
    fun getStrings(): IntArray
    fun getName(): CharSequence
    fun getType(): InstrumentType
}

class GuitarEADGHE : Instrument {
    private val strings = intArrayOf(0, 5, 10, 15, 19, 24)
    private val name = "6-string guitar (E-A-D-G-B-E)"

    override fun getStrings() = strings
    override fun getName(): CharSequence {
        return name
    }
    override fun getType() = InstrumentType.Guitar
}

class BassEADG : Instrument {
    private val strings = intArrayOf(0, 5, 10, 15)
    private val name = "4-string bass (E-A-D-G)"

    override fun getStrings() = strings
    override fun getName(): CharSequence {
        return name
    }
    override fun getType() = InstrumentType.Bass
}

class BassBEADG : Instrument {
    private val strings = intArrayOf(-5, 0, 5, 10, 15)
    private val name = "5-string bass (B-E-A-D-G)"

    override fun getStrings() = strings
    override fun getName(): CharSequence {
        return name
    }
    override fun getType() = InstrumentType.Bass
}

class UkuleleGCEA : Instrument {
    private val strings = intArrayOf(-5, 0, 5, 10)
    private val name = "Ukulele (G-C-E-A)"

    override fun getStrings() = strings
    override fun getName(): CharSequence {
        return name
    }
    override fun getType() = InstrumentType.Ukulele
}


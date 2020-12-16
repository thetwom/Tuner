package de.moekadu.tuner

/// Class containing the preprocessing results.
/**
 * @param size Size of a chunk of sample data which was used to create these results
 * @param sampleRate Sample rate of the chunk of sample data which was used to create these results.
 * @param framePosition Absolute frame number for first value in the underlying sample data chunk.
 */
class TunerResults(val size: Int, val sampleRate: Int, val framePosition: Int) {
    /// Time step width between two data samples
    val dt = 1f / sampleRate

    /// Correlation
    val correlation = FloatArray(size + 1)

    /// Function to compute the time from a index inside the correlation array
    val timeShiftFromCorrelation: (Int) -> (Float) = { i -> i * dt }

    /// Array with correlation times
    val correlationTimes = FloatArray(correlation.size) { i -> timeShiftFromCorrelation(i) }

    /// Spectrum with complex numbers (Even numbers are the real parts, odd numbers are the imaginary parts
    val spectrum = FloatArray(2 * size + 2)

    /// Spectrum with squared amplitudes (these are not the real amplitudes since the spectrum is not normalized)
    val ampSqrSpec = FloatArray(spectrum.size / 2)

    /// Frequency of a given index in the spectrum
    val frequencyFromSpectrum: (Int) -> (Float) = { i -> RealFFT.getFrequency(i, 2 * size, dt) }

    /// Array with correlation times
    val ampSpecSqrFrequencies = FloatArray(ampSqrSpec.size) { i -> frequencyFromSpectrum(i) }

    /// Frequency of a given index in the correlation
    val frequencyFromCorrelation: (Int) -> (Float) = { i -> 1.0f / timeShiftFromCorrelation(i) }

    /// List with indices of maxima in the correlation array.
    var correlationMaximaIndices: ArrayList<Int>? = null
    var specMaximaIndices: ArrayList<Int>? = null

    /// The final pitch frequency.
    var pitchFrequency = -1f
}

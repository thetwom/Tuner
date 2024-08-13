/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.notedetection

/** Result of data in frequency space.
 * @param size Number of different frequencies. This is normally "time_domain_samples + 1"
 * @param df Frequency resolution.
 */
class FrequencySpectrum(
    val size: Int,
    val df: Float,
) {
    /** Array with a frequency for each spectrum value. */
    val frequencies = FloatArray(size) { df * it }
    /** Spectrum, where 2*i is the real part and 2*i+1 is the imaginary part. */
    val spectrum = FloatArray(2 * size)
    /** Squared amplitudes of the spectrum (re*re + im*im) / (numberOfInputSamples * 2f)**2 .*/
    val amplitudeSpectrumSquared = FloatArray(size)

    /** Normalized spectrum for plotting. */
    val plottingSpectrumNormalized = FloatArray(size)

    /** Return the real part of a spectrum value.
     * @param index Index where the value is needed.
     * @return Real part of spectrum at given index.
     */
    fun real(index: Int): Float {
        return spectrum[2 * index]
    }
    /** Return the imaginary part of a spectrum value.
     * @param index Index where the value is needed.
     * @return Imaginary part of spectrum at given index.
     */
    fun imag(index: Int): Float {
        return spectrum[2 * index + 1]
    }
}
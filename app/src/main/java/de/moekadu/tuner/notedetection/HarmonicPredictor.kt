package de.moekadu.tuner.notedetection

/** Predict frequency of a given harmonic, based on previous harmonics.
 * This class uses the function
 *    frequency = f1 * harmonic * (1 + beta * harmonic)
 * which is the same as
 *    frequency = f1 * harmonic + alpha * harmonic**2
 * (with alpha = beta * f1) as modelling function and uses a least squares fit on previous
 * information to predict new harmonics.
 */
class HarmonicPredictor {
    /** Sum of frequency * harmonicNumber */
    private var sumFh = 0f
    /** Sum of frequency * harmonicNumber**2 */
    private var sumFh2 = 0f
    /** Sum of harmonicNumber**2 */
    private var sumH2 = 0f
    /** Sum of harmonicNumber**3 */
    private var sumH3 = 0f
    /** Sum of harmonicNumber**4 */
    private var sumH4 = 0f
    /** Alpha factor of modelling function */
    private var alpha = 0f
    /** Beta factor of modelling function */
    private var beta = 0f
    /** Base frequency of modelling function */
    private var f1 = 0f

    /** Reset predictor. */
    fun clear() {
        sumFh = 0f
        sumFh2 = 0f
        sumH2 = 0f
        sumH3 = 0f
        sumH4 = 0f
        alpha = 0f
        beta = 0f
        f1 = 0f
    }
    /** Add new harmonic to the modelling function.
     * @param harmonicNumber Harmonic number.
     * @param frequency Frequency of harmonic.
     */
    fun add(harmonicNumber: Int, frequency: Float) {
        val hSqr = harmonicNumber * harmonicNumber
        val hCub = harmonicNumber * hSqr
        val hQuad = hSqr * hSqr

        sumFh += frequency * harmonicNumber
        sumFh2 += frequency * hSqr
        sumH2 += hSqr
        sumH3 += hCub
        sumH4 += hQuad
        if (f1 == 0f) {
            f1 = frequency / harmonicNumber
        } else {
            alpha = (sumFh2 * sumH2 - sumFh * sumH3) / (sumH4 * sumH2 - sumH3 * sumH3)
            f1 = (sumFh - alpha * sumH3) / sumH2
            beta = alpha / f1
        }
    }

    /** Predict frequency of a given harmonic.
     * @param harmonicNumber harmonic number.
     * @return Predicted frequency.
     */
    fun predict(harmonicNumber: Int): Float {
        return f1 * harmonicNumber * (1 + beta * harmonicNumber)
    }
}
package de.moekadu.tuner

class TargetNote {
    enum class TuningStatus {TooLow, TooHigh, InTune, Unknown}

    /// Tuning frequency class which connects tone indices with frequencies
    var tuningFrequencies: TuningFrequencies? = null
        set(value) {
            field = value
            invalidate()
        }

    /// Current target note index
    var toneIndex = 0
        set(value) {
            field = value
            invalidate()
        }

    /// Tolerance in cents for a note to be in tune
    var toleranceInCents = 5
        set(value) {
            field = value
            invalidate()
        }

    /// Current note name
    var name: CharSequence? = null
        private set

    /// Frequency of current target note
    var frequency = 0f
        private set
    /// Upper frequency bound for a note to be in tune
    var frequencyUpperTolerance = 0f

        private set
    /// Lower frequency bound for a note to be in tune
    var frequencyLowerTolerance = 0f
        private set

    /// Return the tuning status of a
    // given frequency.
    /**
     * @param currentFrequency Current frequency which should be rated.
     * @return Tuning status of the given frequency.
     */
    fun getTuningStatus(currentFrequency: Float?) = when {
        currentFrequency == null -> TuningStatus.Unknown
        currentFrequency < frequencyLowerTolerance -> TuningStatus.TooLow
        currentFrequency > frequencyUpperTolerance -> TuningStatus.TooHigh
        else -> TuningStatus.InTune
    }

    /// Recompute current target status.
    private fun invalidate() {
        frequency = tuningFrequencies?.getNoteFrequency(toneIndex) ?: 0f
        frequencyLowerTolerance = tuningFrequencies?.getNoteFrequency(
            toneIndex - toleranceInCents / 100f) ?: 0f
        frequencyUpperTolerance = tuningFrequencies?.getNoteFrequency(
            toneIndex + toleranceInCents / 100f) ?: 0f
        name = tuningFrequencies?.getNoteName(frequency)
    }
}
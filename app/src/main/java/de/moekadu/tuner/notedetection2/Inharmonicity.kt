package de.moekadu.tuner.notedetection2

import kotlin.math.log

/** Compute inharmonicity between two frequencies.
 * We define the inharmonicity as f = f_1 * h**(inharmonicity + 1)
 * Such that if two frequencies and corresponding harmonicities are given, we have
 *   inharmonicity = log(fa / fb) / log(ha / hb) - 1
 * which mean that when
 *   - inharmonicity == 0 -> we have the ideal frequency ratio
 *   - inharmonicity  < 0 -> the frequencies are closer together than the ideal frequency ratio.
 *   - inharmonicity  > 0 -> the frequencies are further together than the ideal frequency ratio.
 *
 *   The formula also means that
 *   - inharmonicity ==  1 -> we have twice the ideal stretching
 *   - inharmonicity == -1 -> we have zero stretching
 *
 * @param frequency1 First frequency.
 * @param harmonicNumber1 Harmonic number of first frequency.
 * @param frequency2 Second frequency.
 * @param harmonicNumber2 Harmonic number of second frequency.
 * @return Inharmonicity value.
 */
fun computeInharmonicity(frequency1: Float, harmonicNumber1: Int, frequency2: Float, harmonicNumber2: Int): Float {
    return if (harmonicNumber1 > harmonicNumber2) {
        log(frequency1 / frequency2, harmonicNumber1.toFloat() / harmonicNumber2.toFloat()) - 1
    } else {
        log(frequency2 / frequency1,harmonicNumber2.toFloat() / harmonicNumber1.toFloat()) - 1
    }
}

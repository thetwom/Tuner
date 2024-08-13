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
package de.moekadu.tuner.ui.misc

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

class NumberFormatter(
    locale: Locale,
    private val precision: Int = 4
) {

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols(locale)).apply{
        roundingMode = RoundingMode.HALF_EVEN
    }

    fun format(number: Float): String {
        val numFractionDigits = max(0, precision - floor(log10(number)).toInt())
        decimalFormat.maximumFractionDigits = numFractionDigits
        return decimalFormat.format(number)
    }
}

@Composable
fun rememberNumberFormatter(precision: Int = 4): NumberFormatter {
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocalConfiguration.current.locales[0]
    } else {
        LocalConfiguration.current.locale
    }
    return remember(locale, precision) { NumberFormatter(locale, precision)}
}

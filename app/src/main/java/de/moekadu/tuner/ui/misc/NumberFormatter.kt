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

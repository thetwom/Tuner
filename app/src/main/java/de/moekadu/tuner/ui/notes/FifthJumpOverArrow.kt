package de.moekadu.tuner.ui.notes

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import de.moekadu.tuner.R
import de.moekadu.tuner.temperaments.FifthModification
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlin.math.absoluteValue

/** Generate a string of a fifth correction.
 * @param resources Object for accessing string resources.
 * @param correction Fifth modification to be shown.
 * @return String representation of the fifth correction.
 */
private fun fifthCorrectionString(resources: Resources, correction: FifthModification): String {
    val s = StringBuilder()

    var r = correction.pythagoreanComma
    if (!r.isZero) {
        if (s.isNotEmpty()) {
            if (r.numerator >= 0)
                s.append(resources.getString(R.string.plus_correction))
            else
                s.append(resources.getString(R.string.minus_correction))
        } else if (r.numerator < 0) {
            s.append("-")
        }
        s.append(
            resources.getString(R.string.pythagorean_comma, r.numerator.absoluteValue, r.denominator)
        )
    }

    r = correction.syntonicComma
    if (!r.isZero) {
        if (s.isNotEmpty()) {
            if (r.numerator >= 0)
                s.append(resources.getString(R.string.plus_correction))
            else
                s.append(resources.getString(R.string.minus_correction))
        } else if (r.numerator < 0) {
            s.append("-")
        }
        s.append(
            resources.getString(R.string.syntonic_comma, r.numerator.absoluteValue, r.denominator)
        )
    }

    r = correction.schisma
    if (!r.isZero) {
        if (s.isNotEmpty()) {
            if (r.numerator >= 0)
                s.append(resources.getString(R.string.plus_correction))
            else
                s.append(resources.getString(R.string.minus_correction))
        } else if (r.numerator < 0) {
            s.append("-")
        }
        s.append(
            resources.getString(R.string.schisma, r.numerator.absoluteValue, r.denominator)
        )
    }

    return s.toString()
}

/** An arrow with a fifth correction label on top.
 * @param fifthModification Fifth correction to be shown on top of the arrow.
 * @param modifier Modifier.
 * @param color Color of the arrow and the text.
 * @param style Text style of the label.
 * @param arrowHeight Height of the arrow.
 */
@Composable
fun FifthJumpOverArrow(
    fifthModification: FifthModification,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    arrowHeight: TextUnit = 12.sp
) {
    val resources = LocalContext.current.resources
    val colorResolved = color.takeOrElse {
        LocalContentColor.current.takeOrElse { Color.Black }
    }
    val density = LocalDensity.current
    val arrowHeightDp = with(density) {arrowHeight.toDp()}
    //val arrowHeight = 12.dp
    val arrowWidthDp = ((arrowHeightDp * 43) / 50) // aspect 43:50 according to R.drawable.ic_fifths_arrow
    Column(
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fifthCorrectionString = remember(fifthModification, resources) {
            fifthCorrectionString(resources, fifthModification)
        }
        Text(
            fifthCorrectionString,
            modifier = Modifier.padding(end = arrowWidthDp),
            color = colorResolved,
            style = style
        )
        Row {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_fifths_stroke),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResolved),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(arrowHeightDp)
                    .weight(1f)
            )
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_fifths_arrow),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResolved),
                modifier = Modifier
                    .height(arrowHeightDp)
                    .width(arrowWidthDp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FifthJumpOverArrowPreview() {
    TunerTheme {
        val fifthModification = FifthModification(pythagoreanComma = RationalNumber(-1, 4))
        FifthJumpOverArrow(fifthModification = fifthModification)
    }
}
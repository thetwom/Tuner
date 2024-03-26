package de.moekadu.tuner.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Visualize a fraction.
 * @param numerator Numerator.
 * @param denominator Denominator.
 * @param modifier Modifier.
 * @param fontSize Font size.
 * @param fontStyle Font style.
 * @param color Color.
 * @param fontWeight Font weight.
 * @param fontFamily Font family.
 * @param style Text style.
 */
@Composable
fun Fraction(
    numerator: Int,
    denominator: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val colorResolved = color.takeOrElse {
        style.color.takeOrElse { Color.Black }
    }
    Column(
        modifier = modifier.width(IntrinsicSize.Min),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            numerator.toString(),
            color = colorResolved,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            style = style,
            maxLines = 1
            )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colorResolved)
        )
        Text(
            denominator.toString(),
            color = colorResolved,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            style = style,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FractionPreview() {
    Fraction(
        numerator = 230,
        denominator = 1203
    )
}
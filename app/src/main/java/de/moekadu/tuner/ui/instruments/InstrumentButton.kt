package de.moekadu.tuner.ui.instruments

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.theme.TunerTheme

@Composable
fun InstrumentButton(
    @DrawableRes iconResourceId: Int,
    name: String,
    modifier: Modifier = Modifier,
    outline: PlotWindowOutline = PlotWindowOutline(),
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(outline.cornerRadius),
        border = BorderStroke(outline.lineWidth, outline.color),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.heightIn(
                ButtonDefaults.MinHeight
            ),
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                ImageVector.vectorResource(id = iconResourceId),
                contentDescription = null,
                modifier = Modifier
                    .padding(
                        start = ButtonDefaults.IconSpacing
                    ),
                    //.height(ButtonDefaults.MinHeight)
                    //.aspectRatio(1f)
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                name,
                modifier = Modifier.padding(ButtonDefaults.ContentPadding),
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(widthDp = 300, heightDp = 200, showBackground = true)
@Composable
private fun InstrumentButtonPreview() {
    TunerTheme {
        Column {
            Spacer(
                modifier = Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.error)
            )
            Row {
                InstrumentButton(
                    iconResourceId = R.drawable.ic_piano,
                    name = "My instrument",
                    outline = PlotWindowOutline(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(20.dp).width(10.dp).background(MaterialTheme.colorScheme.error))
            }
            Spacer(
                modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.error)
            )

        }
    }
}

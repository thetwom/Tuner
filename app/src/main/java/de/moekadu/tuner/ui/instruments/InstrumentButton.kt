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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
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
    errorMessage: String? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(outline.cornerRadius),
        border = BorderStroke(
            outline.lineWidth,
            if (errorMessage == null) outline.color else MaterialTheme.colorScheme.error
        ),
        color = if (errorMessage == null)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.errorContainer,
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
                        start = ButtonDefaults.IconSpacing,
                        top = 8.dp,
                        bottom = 8.dp
                    )
                    .size(36.dp)
                    //.height(ButtonDefaults.MinHeight)
                    //.aspectRatio(1f)
                //tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.padding(ButtonDefaults.ContentPadding)
            ) {
                Text(
                    name,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis
                    //color = MaterialTheme.colorScheme.primary
                )
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun InstrumentButtonPreview() {
    TunerTheme {
        Column {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.error)
            )
            Row {
                InstrumentButton(
                    iconResourceId = R.drawable.ic_piano,
                    name = "My instrument",
                    outline = PlotWindowOutline(
                        color = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier
                    .height(20.dp)
                    .width(10.dp)
                    .background(MaterialTheme.colorScheme.error))
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.error)
            )

        }
    }
}

@Preview(widthDp = 300, heightDp = 100, showBackground = true)
@Composable
private fun InstrumentButtonPreview2() {
    TunerTheme {
        Column {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.error)
            )
            Row {
                InstrumentButton(
                    iconResourceId = R.drawable.ic_piano,
                    name = "My instrument 12345678910",
                    outline = PlotWindowOutline(
                        color = MaterialTheme.colorScheme.outline
                    ),
                    errorMessage = "Some error"
                )
                Spacer(modifier = Modifier
                    .height(20.dp)
                    .width(10.dp)
                    .background(MaterialTheme.colorScheme.error))
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.error)
            )

        }
    }
}

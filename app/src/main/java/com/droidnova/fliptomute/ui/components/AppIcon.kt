package com.droidnova.fliptomute.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.droidnova.fliptomute.R

/** How much of the adaptive icon's 108 dp canvas a launcher mask shows: the 72 dp middle. */
const val APP_ICON_MASK_FRACTION = 72f / 108f

/** The rounded tile most launchers use. */
val AppIconShape: Shape = RoundedCornerShape(percent = 23)

/**
 * The app's icon exactly as the home screen draws it: the adaptive foreground cropped to the 72 dp
 * mask and rounded like a launcher tile. Every place that shows the icon (About, the splash) uses
 * this, so the picture is the same everywhere and never shows the canvas around the artwork.
 */
@Composable
fun AppIcon(
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = AppIconShape,
    contentDescription: String? = null,
) {
    Box(modifier.size(size).clip(shape), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = contentDescription,
            // The whole canvas, scaled so the masked part fills the box edge to edge
            modifier = Modifier.requiredSize(size / APP_ICON_MASK_FRACTION),
        )
    }
}

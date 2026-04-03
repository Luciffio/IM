package com.example.im.ui.persona

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * An outgoing message (sent by the player / current user). Right-aligned.
 * Exact port of Reply.kt from the reference.
 *
 * Scale pivot is the right edge, center-Y — same as the reference.
 */
@Composable
fun PersonaOutgoingMessage(entry: PersonaEntry, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text       = entry.message.text,
            style      = MaterialTheme.typography.bodyMedium,
            color      = Color.Black,
            fontFamily = PersonaFont,
            modifier   = Modifier
                .drawWithCache {
                    val outerStemO = with(density) { outgoingOuterStem() }
                        .createOutline(size, layoutDirection, this)
                    val outerBoxO  = with(density) { outgoingOuterBox() }
                        .createOutline(size, layoutDirection, this)
                    val innerStemO = with(density) { outgoingInnerStem() }
                        .createOutline(size, layoutDirection, this)
                    val innerBoxO  = with(density) { outgoingInnerBox() }
                        .createOutline(size, layoutDirection, this)

                    // Pivot = right edge, vertical center.
                    val pivot = Offset(size.width, size.center.y)

                    onDrawBehind {
                        scale(
                            scaleX = entry.messageHorizontalScale.value,
                            scaleY = entry.messageVerticalScale.value,
                            pivot  = pivot,
                        ) {
                            drawOutline(outerBoxO, color = Color.Black)
                            drawOutline(outerStemO, color = Color.Black)
                            drawOutline(innerStemO, color = Color.White)
                            drawOutline(innerBoxO,  color = Color.White)
                        }
                    }
                }
                .alpha(entry.messageTextAlpha.value)
                .padding(start = 44.dp, top = 20.dp, end = 40.dp, bottom = 20.dp),
        )
    }
}

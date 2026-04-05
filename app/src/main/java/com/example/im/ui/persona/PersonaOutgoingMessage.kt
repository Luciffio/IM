package com.example.im.ui.persona

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Outgoing message (current user). Right-aligned within full width.
 * Timestamp badge overlaps the top-right corner of the bubble.
 */
@Composable
fun PersonaOutgoingMessage(
    entry:          PersonaEntry,
    showTimestamps: Boolean  = true,
    modifier:       Modifier = Modifier,
) {
    val density = LocalDensity.current
    val msg     = entry.message

    // Pre-compute bubble outlines once per size.
    val bubbleMod = Modifier
        .drawWithCache {
            val outerStemO = with(density) { outgoingOuterStem() }
                .createOutline(size, layoutDirection, this)
            val outerBoxO  = with(density) { outgoingOuterBox() }
                .createOutline(size, layoutDirection, this)
            val innerStemO = with(density) { outgoingInnerStem() }
                .createOutline(size, layoutDirection, this)
            val innerBoxO  = with(density) { outgoingInnerBox() }
                .createOutline(size, layoutDirection, this)
            val pivot = Offset(size.width, size.center.y)

            onDrawBehind {
                scale(entry.messageHorizontalScale.value, entry.messageVerticalScale.value, pivot) {
                    drawOutline(outerBoxO,  Color.Black)
                    drawOutline(outerStemO, Color.Black)
                    drawOutline(innerStemO, Color.White)
                    drawOutline(innerBoxO,  Color.White)
                }
            }
        }
        .alpha(entry.messageTextAlpha.value)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        // ── Bubble + timestamp badge (top-right) ────────────────────────────
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (msg.hasImage) {
                Column(
                    modifier = Modifier
                        .then(bubbleMod)
                        .padding(start = 44.dp, top = 16.dp, end = 40.dp, bottom = 16.dp),
                ) {
                    val imgShape = with(density) { outgoingInnerBox() }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .drawBehind {
                                val outline = imgShape.createOutline(size, layoutDirection, this)
                                drawOutline(outline, msg.imageColor)
                                drawOutline(
                                    outline = outline,
                                    color   = Color.Black,
                                    style   = Stroke(width = with(density) { 1.dp.toPx() }),
                                )
                            },
                    ) {
                        Text(text = "🖼", fontSize = 30.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = msg.text,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color.Black,
                        fontFamily = PersonaFont,
                    )
                }
            } else {
                Text(
                    text       = msg.text,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = Color.Black,
                    fontFamily = PersonaFont,
                    modifier   = Modifier
                        .then(bubbleMod)
                        .padding(start = 44.dp, top = 20.dp, end = 40.dp, bottom = 20.dp),
                )
            }

            // Timestamp badge overlaps top-right corner of the bubble.
            if (showTimestamps) {
                TimestampBadge(
                    timestamp = msg.timestamp,
                    modifier  = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (-9).dp),
                )
            }
        }
    }
}

package com.example.im.ui.persona

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * Draws the animated black trapezoid connecting [entry1] to [entry2].
 * When [entry2] is null (last entry in the list), nothing is drawn.
 *
 * Shadow: sharp semi-transparent offset copy (P5 style) — no blur.
 * Geometry: exact port of connectingLineModifier.kt from the reference.
 */
fun Modifier.drawConnectingLine(entry1: PersonaEntry, entry2: PersonaEntry?): Modifier {
    if (entry2 == null) return this

    return drawWithCache {
        val topOffset    = TranscriptSizes.getTopDrawingOffset(this, entry1)
        val bottomOffset = TranscriptSizes.getBottomDrawingOffset(this, entry2)

        val topLeft     = entry1.lineCoordinates.leftPoint  + topOffset
        val topRight    = entry1.lineCoordinates.rightPoint + topOffset
        val bottomLeft  = entry2.lineCoordinates.leftPoint  + bottomOffset
        val bottomRight = entry2.lineCoordinates.rightPoint + bottomOffset

        val linePath  = Path()
        // Sharp P5-style shadow: offset (x+3, y+6), no blur, 40% alpha.
        val shadowDx  = 3.dp.toPx()
        val shadowDy  = 6.dp.toPx()
        val shadowColor = Color.Black.copy(alpha = 0.40f)

        onDrawBehind {
            val progress = entry1.lineProgress.value

            val curBottomLeft  = lerp(topLeft,  bottomLeft,  progress)
            val curBottomRight = lerp(topRight, bottomRight, progress)

            with(linePath) {
                rewind()
                moveTo(topLeft.x,        topLeft.y)
                lineTo(topRight.x,       topRight.y)
                lineTo(curBottomRight.x, curBottomRight.y)
                lineTo(curBottomLeft.x,  curBottomLeft.y)
                close()
            }

            // 1. Sharp semi-transparent shadow — same path, shifted right+down.
            translate(left = shadowDx, top = shadowDy) {
                drawPath(linePath, shadowColor)
            }

            // 2. Solid black fill on top.
            drawPath(linePath, Color.Black)
        }
    }
}

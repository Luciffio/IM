package com.example.im.ui.persona

import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur.NORMAL
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * Draws the animated black trapezoid connecting [entry1] to [entry2].
 * When [entry2] is null (last entry in the list), nothing is drawn.
 *
 * Geometry is an exact port of connectingLineModifier.kt from the reference.
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

        val linePath    = Path()
        val shadowPaint = Paint().apply {
            color = Color.Black
            alpha = 0.5f
            asFrameworkPaint().maskFilter = BlurMaskFilter(4.dp.toPx(), NORMAL)
        }

        onDrawBehind {
            val progress = entry1.lineProgress.value

            // Interpolate the bottom endpoints so the line animates downward.
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

            // Blurred shadow offset downward by 16 dp.
            translate(top = 16.dp.toPx()) {
                drawIntoCanvas { it.drawPath(linePath, shadowPaint) }
            }

            drawPath(linePath, Color.Black)
        }
    }
}

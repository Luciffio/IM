package com.example.im.ui.persona

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Top bar with a back arrow (left) and a P5-styled date strip (center).
 *
 * [date] defaults to today; pass a real Telegram chat date when integrating.
 * [onBack] is called when the arrow is tapped — wire to your NavController.
 */
@Composable
fun PersonaTopBar(
    onBack: () -> Unit = {},
    date:   LocalDate  = LocalDate.now(),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
    ) {
        // ── Horizontal black line across full width ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.Center)
                .drawBehind { drawRect(Color.Black) }
        ) {}

        // ── Date badge (centered) ────────────────────────────────────────────
        DateBadge(
            date     = date,
            modifier = Modifier.align(Alignment.Center),
        )

        // ── Back arrow (left) ────────────────────────────────────────────────
        BackArrow(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
        )
    }
}

// ── Date badge ───────────────────────────────────────────────────────────────

@Composable
private fun DateBadge(date: LocalDate, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    // "4 | 3 Th" — month, pipe, day, 2-letter weekday
    val month   = date.monthValue.toString()
    val day     = date.dayOfMonth.toString()
    val weekday = date.format(DateTimeFormatter.ofPattern("EEE")).take(2)

    val label = buildAnnotatedString {
        // month — regular weight
        withStyle(SpanStyle(fontFamily = PersonaFont, fontSize = 15.sp, fontWeight = FontWeight.Normal)) {
            append(month)
        }
        // separator
        withStyle(SpanStyle(fontFamily = PersonaFont, fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Color(0xFF555555))) {
            append("  |  ")
        }
        // day — bold/larger
        withStyle(SpanStyle(fontFamily = PersonaFont, fontSize = 20.sp, fontWeight = FontWeight.Black)) {
            append(day)
        }
        append("  ")
        // weekday abbreviation
        withStyle(SpanStyle(fontFamily = PersonaFont, fontSize = 14.sp, fontWeight = FontWeight.Normal)) {
            append(weekday)
        }
    }

    // White skewed badge — exact style from the reference
    Text(
        text     = label,
        color    = Color.Black,
        modifier = modifier
            .drawWithCache {
                // Outer black parallelogram
                val skew = with(density) { 5.dp.toPx() }
                val outerShape = androidx.compose.foundation.shape.GenericShape { s, _ ->
                    moveTo(skew, 0f)
                    lineTo(s.width - skew, 0f)
                    lineTo(s.width, s.height)
                    lineTo(0f, s.height)
                    close()
                }
                val outerOutline = outerShape.createOutline(size, layoutDirection, this)

                // Inner white parallelogram (inset)
                val pad = with(density) { 2.5.dp.toPx() }
                val innerShape = androidx.compose.foundation.shape.GenericShape { s, _ ->
                    moveTo(skew + pad, pad)
                    lineTo(s.width - skew - pad, pad)
                    lineTo(s.width - pad, s.height - pad)
                    lineTo(pad, s.height - pad)
                    close()
                }
                val innerOutline = innerShape.createOutline(size, layoutDirection, this)

                onDrawBehind {
                    drawOutline(outerOutline, Color.Black)
                    drawOutline(innerOutline, Color.White)
                }
            }
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

// ── Back arrow ───────────────────────────────────────────────────────────────

@Composable
private fun BackArrow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .drawWithCache {
                // P5-style left-pointing angular arrow
                val w   = size.width
                val h   = size.height
                val cx  = w / 2f
                val cy  = h / 2f
                val aw  = w * 0.38f   // half-width of arrowhead
                val ah  = h * 0.28f   // half-height of arrowhead
                val sw  = h * 0.12f   // shaft half-height
                val sl  = w * 0.22f   // shaft length

                val arrowPath = Path().apply {
                    // tip
                    moveTo(cx - aw, cy)
                    // top of arrowhead
                    lineTo(cx, cy - ah)
                    lineTo(cx, cy - sw)
                    // shaft top-right
                    lineTo(cx + aw - sl, cy - sw)
                    lineTo(cx + aw - sl, cy + sw)
                    // shaft bottom
                    lineTo(cx, cy + sw)
                    lineTo(cx, cy + ah)
                    close()
                }

                // Outer black (shadow/border)
                val borderPath = Path().apply {
                    val b = 2.5f
                    moveTo(cx - aw - b, cy)
                    lineTo(cx - b, cy - ah - b)
                    lineTo(cx - b, cy - sw - b)
                    lineTo(cx + aw - sl + b, cy - sw - b)
                    lineTo(cx + aw - sl + b, cy + sw + b)
                    lineTo(cx - b, cy + sw + b)
                    lineTo(cx - b, cy + ah + b)
                    close()
                }

                onDrawBehind {
                    drawPath(borderPath, Color.Black)
                    drawPath(arrowPath, Color.White)
                }
            }
    )
}

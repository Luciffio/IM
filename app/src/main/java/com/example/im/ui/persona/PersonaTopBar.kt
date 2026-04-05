package com.example.im.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full-screen-width P5 TopBar for the chat.
 *
 * Rendered as an OVERLAY (placed in a Box above content in PersonaChatScreen),
 * so chat content scrolls cleanly beneath the diagonal bottom edge.
 *
 * Visual structure:
 *   • Black trapezoid — clips the whole composable via [topBarShape].
 *   • [BackArrow]  — left edge.
 *   • [contactName] in white bold caps + small date line.
 *   • [TopBarAvatar] — white-bordered parallelogram placeholder — right edge.
 */
@Composable
fun PersonaTopBar(
    onBack:         () -> Unit = {},
    date:           LocalDate  = LocalDate.now(),
    contactName:    String     = "PHANTOM THIEVES",
    contactColor:   Color      = P5ColorAnn,
    contactInitial: String     = "P",
) {
    val density = LocalDensity.current

    // clip() to trapezoid + background(Black):
    //   • The composable is visually clipped to the trapezoid shape.
    //   • The transparent "cut" corner exposes chat content below — correct P5 look.
    //   • Works correctly as an overlay: content scrolls under the black area,
    //     not into it.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(with(density) { topBarShape() })
            .background(Color.Black),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
        ) {
            BackArrow(onClick = onBack)
            Spacer(Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text          = contactName.uppercase(),
                    fontFamily    = PersonaFont,
                    fontWeight    = FontWeight.Black,
                    fontSize      = 16.sp,
                    color         = Color.White,
                    letterSpacing = 2.sp,
                    maxLines      = 1,
                )
                TopBarDateLine(date = date)
            }

            TopBarAvatar(
                initial  = contactInitial.uppercase(),
                color    = contactColor,
                modifier = Modifier.padding(end = 8.dp, top = 6.dp, bottom = 10.dp),
            )
        }
    }
}

// ── Date line ─────────────────────────────────────────────────────────────────

@Composable
private fun TopBarDateLine(date: LocalDate) {
    val month   = date.monthValue
    val day     = date.dayOfMonth
    val weekday = date.format(DateTimeFormatter.ofPattern("EEE")).uppercase()
    Text(
        text          = "%02d · %02d  %s".format(month, day, weekday),
        fontFamily    = PersonaFont,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        color         = Color.White.copy(alpha = 0.55f),
        letterSpacing = 1.sp,
    )
}

// ── TopBar avatar ─────────────────────────────────────────────────────────────
//
// Simple parallelogram: white outer path + colored inner path drawn in drawBehind.
// Two filled polygons — no layering, no antialiasing conflicts, no dot artifacts.
// The skew ratio (0.20 × height) matches topBarAvatarShape() from PersonaShapes.kt.

@Composable
private fun TopBarAvatar(
    initial:  String,
    color:    Color,
    modifier: Modifier = Modifier,
) {
    // GenericShape built from size ratios — no Density needed.
    val clipShape = remember {
        GenericShape { size, _ ->
            val skew = size.height * 0.20f
            moveTo(skew, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - skew, size.height)
            lineTo(0f, size.height)
            close()
        }
    }

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 42.dp)
            .drawBehind {
                // 2.5 dp border width — uses DrawScope's own Density, no LocalDensity needed.
                val bw   = 2.5.dp.toPx()
                val skew = size.height * 0.20f

                // Outer parallelogram — white (border)
                val outerPath = Path().apply {
                    moveTo(skew, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width - skew, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                // Inner parallelogram — contact color (inset by bw on all sides)
                val innerPath = Path().apply {
                    moveTo(skew + bw, bw)
                    lineTo(size.width - bw, bw)
                    lineTo(size.width - skew - bw, size.height - bw)
                    lineTo(bw, size.height - bw)
                    close()
                }

                drawPath(outerPath, Color.White)
                drawPath(innerPath, color)
            }
            .clip(clipShape),
    ) {
        Text(
            text       = initial,
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize   = 15.sp,
            color      = Color.Black,
            modifier   = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 4.dp),
        )
    }
}

// ── Back arrow ────────────────────────────────────────────────────────────────

@Composable
internal fun BackArrow(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                val w  = size.width
                val h  = size.height
                val cx = w / 2f
                val cy = h / 2f
                val aw = w * 0.38f
                val ah = h * 0.28f
                val sw = h * 0.12f
                val sl = w * 0.22f

                val arrowPath = Path().apply {
                    moveTo(cx - aw, cy)
                    lineTo(cx, cy - ah)
                    lineTo(cx, cy - sw)
                    lineTo(cx + aw - sl, cy - sw)
                    lineTo(cx + aw - sl, cy + sw)
                    lineTo(cx, cy + sw)
                    lineTo(cx, cy + ah)
                    close()
                }
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
    ) {}
}

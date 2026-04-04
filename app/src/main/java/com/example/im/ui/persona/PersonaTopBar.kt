package com.example.im.ui.persona

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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full-screen-width P5 TopBar for the chat.
 *
 * Visual structure (back-to-front):
 *   • Black trapezoid background drawn via [topBarShape] — overlaps status bar.
 *   • [BackArrow]  — left edge.
 *   • [contactName] in white bold caps + small date line.
 *   • [TopBarAvatar] — parallelogram card with 1-dp white border — right edge.
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

    // Outer Box: no fixed height — grows with statusBar + 64 dp content.
    // drawBehind runs BEFORE the inner content, so the trapezoid covers the full
    // height including the system status bar area.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val outline = with(density) { topBarShape() }
                    .createOutline(size, layoutDirection, this)
                drawOutline(outline, Color.Black)
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
        ) {
            // ── Back arrow ───────────────────────────────────────────────────
            BackArrow(onClick = onBack)

            Spacer(Modifier.width(8.dp))

            // ── Contact name + date ──────────────────────────────────────────
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

            // ── Avatar ───────────────────────────────────────────────────────
            TopBarAvatar(
                initial = contactInitial.uppercase(),
                color   = contactColor,
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
// Exact same visual as PersonaAvatar (black→white→colored layers) using the
// proportional shapes from PersonaShapes.kt — scales to any size.

@Composable
private fun TopBarAvatar(
    initial:  String,
    color:    Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .size(width = 58.dp, height = 47.dp)
            .drawBehind {
                drawOutline(
                    with(density) { avatarScaledBlackBox() }
                        .createOutline(size, layoutDirection, this),
                    Color.Black,
                )
                drawOutline(
                    with(density) { avatarScaledWhiteBox() }
                        .createOutline(size, layoutDirection, this),
                    Color.White,
                )
                drawOutline(
                    with(density) { avatarScaledColoredBox() }
                        .createOutline(size, layoutDirection, this),
                    color,
                )
            }
            .clip(with(density) { avatarScaledClipBox() }),
    ) {
        // Portrait initial — top-end, same anchor as PersonaAvatar portrait image
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

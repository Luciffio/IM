package com.example.im.ui.persona

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Incoming message (other participant). Renders:
 *  - [PersonaAvatar] on the left
 *  - [IncomingTextBox] overlapping to the right (with optional image)
 *  - [TimestampLabel] floating below — "margin note" aesthetic
 */
@Composable
fun PersonaIncomingMessage(entry: PersonaEntry, modifier: Modifier = Modifier) {
    Box {
        IncomingEntryLayout(
            avatar   = { PersonaAvatar(entry) },
            textBox  = { IncomingTextBox(entry) },
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .then(modifier),
        )

        // Timestamp floats in the gap between list items — chaotic margin-note look.
        TimestampLabel(
            timestamp  = entry.message.timestamp,
            msgId      = entry.message.id,
            isOutgoing = false,
            modifier   = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp)
                .offset(y = 8.dp),
        )
    }
}

// ── Entry layout (avatar + text box side-by-side) ─────────────────────────────

@Composable
private fun IncomingEntryLayout(
    avatar:   @Composable () -> Unit,
    textBox:  @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content  = { avatar(); textBox() },
        modifier = modifier,
    ) { (avatarM, textM), constraints ->
        val textBoxOverlap    = 18.dp.roundToPx()
        val textBoxTopPadding = 4.dp.roundToPx()

        val avatarP  = avatarM.measure(constraints)
        val textMaxW = constraints.maxWidth - avatarP.width + textBoxOverlap
        val textP    = textM.measure(constraints.copy(maxWidth = textMaxW))

        val textH  = textP.height + textBoxTopPadding
        val width  = avatarP.width + textP.width - textBoxOverlap
        val height = maxOf(avatarP.height, textH)

        layout(width, height) {
            avatarP.place(0, 0)
            val textBoxX = avatarP.width - textBoxOverlap
            val textBoxY = if (textH > avatarP.height) textBoxTopPadding
                           else height - textP.height - 6.dp.roundToPx()
            textP.place(textBoxX, textBoxY)
        }
    }
}

// ── Speech bubble ─────────────────────────────────────────────────────────────

/**
 * Draws the P5 parallelogram bubble behind the content.
 * When [entry.message.hasImage] is true, shows a placeholder image clipped to
 * the same [incomingInnerBox] shape as the bubble interior.
 */
@Composable
private fun IncomingTextBox(entry: PersonaEntry) {
    val density = LocalDensity.current
    val msg     = entry.message

    // Common bubble-drawing modifier — adapts to whatever size the composable ends up.
    val bubbleMod = Modifier
        .drawWithCache {
            val stemY      = with(density) { incomingGetStemY(size.height) }
            val outerStemO = with(density) { incomingOuterStem(stemY) }
                .createOutline(size, layoutDirection, this)
            val outerBoxO  = with(density) { incomingOuterBox() }
                .createOutline(size, layoutDirection, this)
            val innerStemO = with(density) { incomingInnerStem(stemY) }
                .createOutline(size, layoutDirection, this)
            val innerBoxO  = with(density) { incomingInnerBox() }
                .createOutline(size, layoutDirection, this)
            val pivot = Offset(x = outerStemO.bounds.width, y = stemY)

            onDrawBehind {
                scale(entry.messageHorizontalScale.value, entry.messageVerticalScale.value, pivot) {
                    drawOutline(outerBoxO, Color.White)
                }
                drawOutline(outerStemO, Color.White)
                drawOutline(innerStemO, Color.Black)
                scale(entry.messageHorizontalScale.value, entry.messageVerticalScale.value, pivot) {
                    drawOutline(innerBoxO, Color.Black)
                }
            }
        }
        .alpha(entry.messageTextAlpha.value)

    // Wrap in Box so punctuation mark can be overlaid exactly as in reference Entry.kt
    Box {
        if (msg.hasImage) {
            Column(
                modifier = bubbleMod
                    .padding(start = 42.dp, top = 16.dp, end = 32.dp, bottom = 16.dp),
            ) {
                // Image placeholder — clipped to inner-box parallelogram + white 1-dp border
                val imgShape = with(density) { incomingInnerBox() }
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
                                color   = Color.White,
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
                    color      = Color.White,
                    fontFamily = PersonaFont,
                )
            }
        } else {
            Text(
                text       = msg.text,
                style      = MaterialTheme.typography.bodyMedium,
                color      = Color.White,
                fontFamily = PersonaFont,
                modifier   = bubbleMod
                    .padding(start = 42.dp, top = 20.dp, end = 32.dp, bottom = 20.dp),
            )
        }

        // Punctuation mark — reference Entry.kt:127 uses R.drawable.question_mark here;
        // we use a styled Text fallback until the drawable asset is added.
        if (entry.drawPunctuation) {
            val s = entry.punctuationScale.value
            Text(
                text       = "?",
                fontFamily = PersonaFont,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                fontSize   = 22.sp,
                color      = Color.White,
                modifier   = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = (-16).dp)
                    .graphicsLayer { scaleX = s; scaleY = s },
            )
        }
    }
}

// ── Timestamp label ───────────────────────────────────────────────────────────

/**
 * Small italic timestamp rendered OUTSIDE the bubble — floats in the gap
 * between items. Each message gets a unique tilt and x-offset derived from
 * its ID so they look like scattered margin notes.
 */
@Composable
internal fun TimestampLabel(
    timestamp:  Long,
    msgId:      Long,
    isOutgoing: Boolean,
    modifier:   Modifier = Modifier,
) {
    val time = remember(timestamp) {
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    // Pseudo-random tilt: −3.2° … +3.2°, direction flips for outgoing.
    val rawTilt = ((msgId % 9) - 4) * 0.8f
    val tilt    = if (isOutgoing) -rawTilt else rawTilt
    val xShift  = ((msgId % 5) - 2).toInt().dp

    Text(
        text       = time,
        fontFamily = PersonaFont,
        fontStyle  = FontStyle.Italic,
        fontSize   = 10.sp,
        color      = Color.White.copy(alpha = 0.70f),
        modifier   = modifier
            .offset(x = xShift)
            .rotate(tilt),
    )
}

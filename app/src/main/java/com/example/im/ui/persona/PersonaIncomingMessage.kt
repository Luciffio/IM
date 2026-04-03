package com.example.im.ui.persona

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * An incoming message (sent by another participant). Renders:
 *  - [PersonaAvatar] on the left
 *  - [IncomingTextBox] overlapping it to the right
 *
 * The custom [Layout] exactly mirrors EntryLayout from Entry.kt in the reference.
 */
@Composable
fun PersonaIncomingMessage(entry: PersonaEntry, modifier: Modifier = Modifier) {
    IncomingEntryLayout(
        avatar  = { PersonaAvatar(entry) },
        textBox = { IncomingTextBox(entry) },
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .then(modifier),
    )
}

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
        // How much the text box overlaps the avatar to hide the stem root.
        val textBoxOverlap    = 18.dp.roundToPx()
        val textBoxTopPadding = 4.dp.roundToPx()

        val avatarP   = avatarM.measure(constraints)
        val textMaxW  = constraints.maxWidth - avatarP.width + textBoxOverlap
        val textP     = textM.measure(constraints.copy(maxWidth = textMaxW))

        val textH     = textP.height + textBoxTopPadding
        val width     = avatarP.width + textP.width - textBoxOverlap
        val height    = maxOf(avatarP.height, textH)

        layout(width, height) {
            avatarP.place(0, 0)

            val textBoxX = avatarP.width - textBoxOverlap
            val textBoxY = if (textH > avatarP.height) {
                textBoxTopPadding
            } else {
                height - textP.height - 6.dp.roundToPx()
            }
            textP.place(textBoxX, textBoxY)
        }
    }
}

/**
 * The actual speech-bubble drawn behind the message text.
 * Drawing order (back-to-front): outerBox → outerStem → innerStem → innerBox.
 * Scales horizontally/vertically from the stem anchor, exactly as in TextBox from Entry.kt.
 */
@Composable
private fun IncomingTextBox(entry: PersonaEntry) {
    val density = LocalDensity.current

    Text(
        text  = entry.message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White,
        fontFamily = PersonaFont,
        modifier = Modifier
            .drawWithCache {
                // All outlines pre-computed once per size change.
                val stemY      = with(density) { incomingGetStemY(size.height) }
                val outerStemO = with(density) { incomingOuterStem(stemY) }
                    .createOutline(size, layoutDirection, this)
                val outerBoxO  = with(density) { incomingOuterBox() }
                    .createOutline(size, layoutDirection, this)
                val innerStemO = with(density) { incomingInnerStem(stemY) }
                    .createOutline(size, layoutDirection, this)
                val innerBoxO  = with(density) { incomingInnerBox() }
                    .createOutline(size, layoutDirection, this)

                // Pivot for the scale animation = left edge of box, vertically at stemY.
                val pivot = Offset(x = outerStemO.bounds.width, y = stemY)

                onDrawBehind {
                    // Outer white box scales in.
                    scale(
                        scaleX = entry.messageHorizontalScale.value,
                        scaleY = entry.messageVerticalScale.value,
                        pivot  = pivot,
                    ) {
                        drawOutline(outerBoxO, color = Color.White)
                    }

                    // Stem does NOT scale — drawn at full size always.
                    drawOutline(outerStemO, color = Color.White)
                    drawOutline(innerStemO, color = Color.Black)

                    // Inner black box scales in (same pivot as outer).
                    scale(
                        scaleX = entry.messageHorizontalScale.value,
                        scaleY = entry.messageVerticalScale.value,
                        pivot  = pivot,
                    ) {
                        drawOutline(innerBoxO, color = Color.Black)
                    }
                }
            }
            .alpha(entry.messageTextAlpha.value)
            .padding(start = 42.dp, top = 20.dp, end = 32.dp, bottom = 20.dp)
    )
}

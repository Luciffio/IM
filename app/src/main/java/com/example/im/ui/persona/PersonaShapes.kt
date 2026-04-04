package com.example.im.ui.persona

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// All geometry is copied pixel-perfect from the Persona-IM reference project.
// Every coordinate corresponds to an exact dp value from the original source.
// ---------------------------------------------------------------------------

// ── Shared sizes ────────────────────────────────────────────────────────────

object PersonaSizes {
    val AvatarSize      = DpSize(110.dp, 90.dp)
    val EntrySpacing    = 16.dp
    // X/Y center of the outgoing (Ren) message connecting-line anchor.
    val OutgoingCenterX = 60.dp
    val OutgoingCenterY = 28.dp
    // Random width range for the connecting line.
    val MinLineWidth    = 44.dp
    val MaxLineWidth    = 60.dp
    // Random horizontal jitter applied to each line segment endpoint.
    val MinLineShift    = 16.dp
    val MaxLineShift    = 48.dp
}

// ── Incoming message shapes (Entry / TextBox) ────────────────────────────────

/** Outer black parallelogram of the incoming text bubble. */
internal fun Density.incomingOuterBox(): Shape = GenericShape { size, _ ->
    moveTo(31.7.dp.toPx(), 3.1.dp.toPx())
    lineTo(size.width, 0f)
    lineTo(size.width - 23.dp.toPx(), size.height)
    lineTo(15.6.dp.toPx(), size.height - 8.dp.toPx())
    close()
}

/** Inner white parallelogram — sits on top of the outer box. */
internal fun Density.incomingInnerBox(): Shape = GenericShape { size, _ ->
    moveTo(33.dp.toPx(),              7.7.dp.toPx())
    lineTo(size.width - 13.dp.toPx(), 3.7.dp.toPx())
    lineTo(size.width - 25.7.dp.toPx(), size.height - 4.6.dp.toPx())
    lineTo(20.4.dp.toPx(),            size.height - 12.dp.toPx())
    close()
}

/**
 * Outer (white) stem that connects the text box to the avatar.
 * [stemY] must be computed via [incomingGetStemY] before calling.
 */
internal fun Density.incomingOuterStem(stemY: Float): Shape = GenericShape { _, _ ->
    moveTo(0f,                 stemY - 19.2.dp.toPx())
    lineTo(19.5.dp.toPx(),    stemY - 37.2.dp.toPx())
    lineTo(20.8.dp.toPx(),    stemY - 31.5.dp.toPx())
    lineTo(32.4.dp.toPx(),    stemY - 39.3.dp.toPx())
    lineTo(30.6.dp.toPx(),    stemY - 15.8.dp.toPx())
    lineTo(11.7.dp.toPx(),    stemY - 12.6.dp.toPx())
    lineTo(10.dp.toPx(),      stemY - 20.dp.toPx())
    close()
}

/** Inner (black) stem — inset on top of the outer stem. */
internal fun Density.incomingInnerStem(stemY: Float): Shape = GenericShape { _, _ ->
    moveTo(4.6.dp.toPx(),     stemY - 22.2.dp.toPx())
    lineTo(17.dp.toPx(),      stemY - 33.2.dp.toPx())
    lineTo(19.3.dp.toPx(),    stemY - 28.1.dp.toPx())
    lineTo(34.4.dp.toPx(),    stemY - 36.5.dp.toPx())
    lineTo(34.dp.toPx(),      stemY - 21.4.dp.toPx())
    lineTo(14.4.dp.toPx(),    stemY - 18.6.dp.toPx())
    lineTo(12.8.dp.toPx(),    stemY - 25.4.dp.toPx())
    close()
}

/**
 * Derives the correct stem Y anchor depending on whether the text box is taller
 * than the avatar (box is top-anchored) or shorter (box is bottom-anchored).
 * Mirrors [getStemY] from the reference Entry.kt exactly.
 */
internal fun Density.incomingGetStemY(textBoxHeightPx: Float): Float {
    val avatarHeightPx = PersonaSizes.AvatarSize.height.toPx()
    return if (textBoxHeightPx + 4.dp.toPx() > avatarHeightPx) {
        avatarHeightPx - 16.dp.toPx()
    } else {
        textBoxHeightPx - 5.dp.toPx()
    }
}

// ── Outgoing message shapes (Reply) ─────────────────────────────────────────

/** Outer black parallelogram of the outgoing (player) bubble. */
internal fun Density.outgoingOuterBox(): Shape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width - 35.dp.toPx(),      4.dp.toPx())
    lineTo(size.width - 10.7.dp.toPx(),    size.height - 6.6.dp.toPx())
    lineTo(35.5.dp.toPx(),                 size.height)
    close()
}

/** Inner white parallelogram of the outgoing bubble. */
internal fun Density.outgoingInnerBox(): Shape = GenericShape { size, _ ->
    moveTo(12.dp.toPx(),                   5.dp.toPx())
    lineTo(size.width - 36.dp.toPx(),      9.5.dp.toPx())
    lineTo(size.width - 16.4.dp.toPx(),    size.height - 11.7.dp.toPx())
    lineTo(36.5.dp.toPx(),                 size.height - 3.5.dp.toPx())
    close()
}

/** Outer black stem of the outgoing bubble — points to the right edge. */
internal fun Density.outgoingOuterStem(): Shape = GenericShape { size, _ ->
    val v = size.height        // vertical origin
    moveTo(size.width - 37.6.dp.toPx(), v - 42.3.dp.toPx())
    lineTo(size.width - 20.8.dp.toPx(), v - 30.2.dp.toPx())
    lineTo(size.width - 19.4.dp.toPx(), v - 36.8.dp.toPx())
    lineTo(size.width,                  v - 19.6.dp.toPx())
    lineTo(size.width - 10.3.dp.toPx(), v - 19.6.dp.toPx())
    lineTo(size.width - 12.dp.toPx(),   v - 12.3.dp.toPx())
    lineTo(size.width - 27.6.dp.toPx(), v - 15.2.dp.toPx())
    close()
}

/** Inner white stem of the outgoing bubble — sits on top of the outer stem. */
internal fun Density.outgoingInnerStem(): Shape = GenericShape { size, _ ->
    val v = size.height
    moveTo(size.width - 33.1.dp.toPx(), v - 33.2.dp.toPx())
    lineTo(size.width - 19.3.dp.toPx(), v - 26.3.dp.toPx())
    lineTo(size.width - 16.4.dp.toPx(), v - 31.6.dp.toPx())
    lineTo(size.width - 4.2.dp.toPx(),  v - 21.dp.toPx())
    lineTo(size.width - 12.4.dp.toPx(), v - 23.4.dp.toPx())
    lineTo(size.width - 14.dp.toPx(),   v - 17.2.dp.toPx())
    lineTo(size.width - 28.6.dp.toPx(), v - 21.2.dp.toPx())
    close()
}

// ── Avatar shapes ────────────────────────────────────────────────────────────
// All shapes use absolute dp coordinates that match the 110×90 dp avatar size.

/** Outermost black box behind the avatar. */
internal fun Density.avatarBlackBox(): Shape = GenericShape { _, _ ->
    moveTo(0f,                  17.dp.toPx())
    lineTo(100.5.dp.toPx(),     27.2.dp.toPx())
    lineTo(110.dp.toPx(),       72.7.dp.toPx())
    lineTo(33.4.dp.toPx(),      90.dp.toPx())
    close()
}

/** White box sits between the black and colored boxes. */
internal fun Density.avatarWhiteBox(): Shape = GenericShape { _, _ ->
    moveTo(16.4.dp.toPx(),      20.5.dp.toPx())
    lineTo(96.7.dp.toPx(),      30.4.dp.toPx())
    lineTo(106.4.dp.toPx(),     70.dp.toPx())
    lineTo(37.8.dp.toPx(),      80.4.dp.toPx())
    close()
}

/** Colored accent box whose color comes from the sender's [ChatParticipant.color]. */
internal fun Density.avatarColoredBox(): Shape = GenericShape { _, _ ->
    moveTo(22.5.dp.toPx(),      28.dp.toPx())
    lineTo(94.4.dp.toPx(),      31.4.dp.toPx())
    lineTo(104.3.dp.toPx(),     67.5.dp.toPx())
    lineTo(40.dp.toPx(),        76.6.dp.toPx())
    close()
}

/** Clip shape so the portrait image is masked to the visible avatar area. */
internal fun Density.avatarClipBox(): Shape = GenericShape { _, _ ->
    moveTo(10.3.dp.toPx(),      (-5.6).dp.toPx())
    lineTo(114.7.dp.toPx(),     (-5.6).dp.toPx())
    lineTo(114.7.dp.toPx(),     65.6.dp.toPx())
    lineTo(40.dp.toPx(),        76.6.dp.toPx())
    close()
}

// ── Proportional avatar shapes ────────────────────────────────────────────────
// Coordinates normalised from the original 110×90 dp avatar.
// These scale to any Box size, so the same visual can be reused in the TopBar.

internal fun Density.avatarScaledBlackBox(): Shape = GenericShape { size, _ ->
    moveTo(0f,                   size.height * 0.189f)
    lineTo(size.width * 0.914f,  size.height * 0.302f)
    lineTo(size.width,           size.height * 0.808f)
    lineTo(size.width * 0.304f,  size.height)
    close()
}

internal fun Density.avatarScaledWhiteBox(): Shape = GenericShape { size, _ ->
    moveTo(size.width * 0.149f,  size.height * 0.228f)
    lineTo(size.width * 0.879f,  size.height * 0.338f)
    lineTo(size.width * 0.967f,  size.height * 0.778f)
    lineTo(size.width * 0.344f,  size.height * 0.893f)
    close()
}

internal fun Density.avatarScaledColoredBox(): Shape = GenericShape { size, _ ->
    moveTo(size.width * 0.205f,  size.height * 0.311f)
    lineTo(size.width * 0.858f,  size.height * 0.349f)
    lineTo(size.width * 0.948f,  size.height * 0.750f)
    lineTo(size.width * 0.364f,  size.height * 0.851f)
    close()
}

/** Clip mask for the portrait initial — mirrors avatarClipBox proportionally. */
internal fun Density.avatarScaledClipBox(): Shape = GenericShape { size, _ ->
    moveTo(size.width * 0.094f,  size.height * (-0.062f))
    lineTo(size.width * 1.043f,  size.height * (-0.062f))
    lineTo(size.width * 1.043f,  size.height * 0.729f)
    lineTo(size.width * 0.364f,  size.height * 0.851f)
    close()
}

// ── TopBar shapes ─────────────────────────────────────────────────────────────

/**
 * Full-width black trapezoid for the chat TopBar.
 * Left bottom sits at full height; right bottom is raised by 20 dp — same lean
 * direction as the message bubbles.
 */
internal fun Density.topBarShape(): Shape = GenericShape { size, _ ->
    val raise = 20.dp.toPx()
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - raise)
    lineTo(0f, size.height)
    close()
}

/**
 * Parallelogram for the TopBar contact avatar — skewed to match the bubble lean.
 */
internal fun Density.topBarAvatarShape(): Shape = GenericShape { size, _ ->
    val skew = size.height * 0.20f
    moveTo(skew, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - skew, size.height)
    lineTo(0f, size.height)
    close()
}

// ── Reaction badge shape ──────────────────────────────────────────────────────

/**
 * Sharp parallelogram for reaction sticker badges.
 * No rounded corners — pure P5 geometry.
 */
internal fun Density.reactionBadgeShape(): Shape = GenericShape { size, _ ->
    val skew = 4.dp.toPx()
    moveTo(skew, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - skew, size.height)
    lineTo(0f, size.height)
    close()
}

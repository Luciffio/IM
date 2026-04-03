package com.example.im.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the sender's avatar — the layered parallelogram card that appears
 * to the left of an incoming message. Matches Avatar.kt from the reference.
 *
 * The portrait image is a colored placeholder; replace the [AvatarPortrait]
 * composable call below with a real image loader (Coil / Glide) once you have
 * actual Telegram profile photos.
 */
@Composable
fun PersonaAvatar(entry: PersonaEntry) {
    val density     = LocalDensity.current
    val participant = entry.participant ?: return  // never rendered for outgoing

    Box(
        modifier = Modifier
            .size(PersonaSizes.AvatarSize)
            .scale(entry.avatarBackgroundScale.value)
            .drawBehind {
                drawOutline(
                    outline = with(density) { avatarBlackBox() }
                        .createOutline(size, layoutDirection, this),
                    color   = Color.Black,
                )
                drawOutline(
                    outline = with(density) { avatarWhiteBox() }
                        .createOutline(size, layoutDirection, this),
                    color   = Color.White,
                )
                drawOutline(
                    outline = with(density) { avatarColoredBox() }
                        .createOutline(size, layoutDirection, this),
                    color   = participant.color,
                )
            }
            .clip(with(density) { avatarClipBox() })
    ) {
        AvatarPortrait(
            participant = participant,
            scale       = entry.avatarForegroundScale.value,
            modifier    = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 8.dp),
        )
    }
}

/**
 * Placeholder portrait.
 *
 * Replace with an AsyncImage / painterResource once real images are available.
 * The scale animation is applied as a graphicsLayer in the parent to keep the
 * pivot at the bottom-center (matching the reference's TransformOrigin(0.5, 1.15)).
 */
@Composable
private fun AvatarPortrait(
    participant: ChatParticipant,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scaleX = scale, scaleY = scale)
            .background(participant.color.copy(alpha = 0.6f)),
    ) {
        Text(
            text       = participant.name.first().toString(),
            color      = Color.Black,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

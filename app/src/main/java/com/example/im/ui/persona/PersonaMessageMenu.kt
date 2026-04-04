package com.example.im.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Actions
// ---------------------------------------------------------------------------

enum class MessageAction(val icon: ImageVector, val label: String) {
    Reply    (Icons.Default.Reply,       "Reply"),
    Edit     (Icons.Default.Edit,        "Edit"),
    Pin      (Icons.Default.PushPin,     "Pin"),
    CopyText (Icons.Default.ContentCopy, "Copy Text"),
    Forward  (Icons.Default.Forward,     "Forward"),
    Delete   (Icons.Default.Delete,      "Delete"),
    Select   (Icons.Default.CheckBox,    "Select"),
}

private val QuickReactions = listOf("👌", "❤️", "👍", "👎", "🔥", "😍")

// P5-style accent bar colors — bold stripes across the card top
private val AccentColors = listOf(
    Color(0xFFCC0000),   // P5 red
    Color(0xFFFF4081),   // hot pink
    Color(0xFF1565C0),   // blue
    Color(0xFFFFC107),   // amber/yellow
    Color(0xFF00BFA5),   // teal/green
)

// ---------------------------------------------------------------------------
// Menu overlay
// ---------------------------------------------------------------------------

/**
 * Full-screen dimmed overlay with P5-styled white message context menu.
 * Tap outside the card to dismiss.
 */
@Composable
fun PersonaMessageMenu(
    entry:           PersonaEntry,
    onDismiss:       () -> Unit,
    onReact:         (String) -> Unit,
    onAction:        (MessageAction) -> Unit,
    onExpandEmoji:   () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val actions   = buildList {
        add(MessageAction.Reply)
        if (entry.message.isOutgoing) add(MessageAction.Edit)
        add(MessageAction.Pin)
        add(MessageAction.CopyText)
        add(MessageAction.Forward)
        add(MessageAction.Delete)
        add(MessageAction.Select)
    }

    val time = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(entry.message.timestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    // Dim background — tap to dismiss
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onDismiss,
            ),
    ) {
        MenuCard {
            // ── P5 accent bar (colored diagonal stripes) ─────────────────────
            P5AccentBar()

            // ── Quick reactions ──────────────────────────────────────────────
            QuickReactionRow(
                onReact  = { emoji -> onReact(emoji); onDismiss() },
                onExpand = { onExpandEmoji(); onDismiss() },
            )

            HorizontalDivider(color = Color(0xFFDDDDDD), thickness = 1.dp)

            // ── Action items ─────────────────────────────────────────────────
            actions.forEachIndexed { index, action ->
                ActionItem(
                    action  = action,
                    onClick = {
                        if (action == MessageAction.CopyText) {
                            clipboard.setText(AnnotatedString(entry.message.text))
                        }
                        onAction(action)
                        onDismiss()
                    },
                )
                if (index < actions.lastIndex) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                }
            }

            HorizontalDivider(color = Color(0xFFDDDDDD), thickness = 1.dp)

            // ── Timestamp ────────────────────────────────────────────────────
            Text(
                text       = "✓✓  today at $time",
                fontFamily = PersonaFont,
                fontSize   = 11.sp,
                color      = Color(0xFF999999),
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Card
// ---------------------------------------------------------------------------

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    // Parallelogram card — slight rightward lean, P5 style
    val cardShape = GenericShape { size, _ ->
        val skew = size.height * 0.012f   // very subtle lean
        moveTo(skew, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width - skew, size.height)
        lineTo(0f, size.height)
        close()
    }

    Column(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 310.dp)
            // Stop tap propagation so clicking card doesn't dismiss overlay
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = {},
            )
            .clip(cardShape)
            .background(Color.White),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// P5 accent bar — diagonal colored stripes
// ---------------------------------------------------------------------------

@Composable
private fun P5AccentBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .drawWithCache {
                val stripeCount = AccentColors.size
                val segW = size.width / stripeCount
                val lean = size.height * 0.5f  // how much each stripe leans right

                // Build one path per stripe
                val paths = AccentColors.mapIndexed { i, _ ->
                    Path().apply {
                        val x0 = i * segW - lean
                        val x1 = (i + 1) * segW - lean
                        moveTo(x0 + lean, 0f)
                        lineTo(x1 + lean, 0f)
                        lineTo(x1, size.height)
                        lineTo(x0, size.height)
                        close()
                    }
                }

                onDrawBehind {
                    paths.forEachIndexed { i, path ->
                        drawPath(path, AccentColors[i])
                    }
                }
            },
    )
}

// ---------------------------------------------------------------------------
// Quick reaction row
// ---------------------------------------------------------------------------

@Composable
private fun QuickReactionRow(
    onReact:  (String) -> Unit,
    onExpand: () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        QuickReactions.forEach { emoji ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { onReact(emoji) },
                    ),
            ) {
                Text(text = emoji, fontSize = 24.sp)
            }
        }

        // Expand to full emoji panel
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onExpand,
                ),
        ) {
            Text(text = "▼", fontSize = 13.sp, color = Color(0xFF888888))
        }
    }
}

// ---------------------------------------------------------------------------
// Action item row
// ---------------------------------------------------------------------------

@Composable
private fun ActionItem(action: MessageAction, onClick: () -> Unit) {
    val isDelete  = action == MessageAction.Delete
    val textColor = if (isDelete) Color(0xFFE53935) else Color(0xFF1A1A1A)
    val iconColor = if (isDelete) Color(0xFFE53935) else Color(0xFF444444)

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector        = action.icon,
            contentDescription = action.label,
            tint               = iconColor,
            modifier           = Modifier.size(20.dp),
        )
        Text(
            text       = action.label,
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Bold,
            fontSize   = 15.sp,
            color      = textColor,
        )
    }
}

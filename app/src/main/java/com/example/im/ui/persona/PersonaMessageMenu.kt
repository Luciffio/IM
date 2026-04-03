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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
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

enum class MessageAction(val icon: String, val label: String) {
    Reply    ("↩",  "Reply"),
    Edit     ("✏",  "Edit"),
    Pin      ("📌", "Pin"),
    CopyText ("⎘",  "Copy Text"),
    Forward  ("↪",  "Forward"),
    Delete   ("🗑",  "Delete"),
    Select   ("☑",  "Select"),
}

private val QuickReactions = listOf("👌", "❤️", "👍", "👎", "🔥", "😍")

// ---------------------------------------------------------------------------
// Menu overlay
// ---------------------------------------------------------------------------

/**
 * Full-screen dimmed overlay with the P5-styled message context menu.
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

    // Dim background — tap to dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onDismiss,
            ),
    ) {
        // Card — centered, non-propagating clicks
        MenuCard(
            modifier = Modifier.align(Alignment.Center),
        ) {
            // ── Quick reactions ──────────────────────────────────────────────
            QuickReactionRow(
                onReact      = { emoji ->
                    onReact(emoji)
                    onDismiss()
                },
                onExpand     = {
                    onExpandEmoji()
                    onDismiss()
                },
            )

            HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

            // ── Action items ─────────────────────────────────────────────────
            actions.forEach { action ->
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
                if (action != actions.last()) {
                    HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
                }
            }

            HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

            // ── Timestamp ────────────────────────────────────────────────────
            val time = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(entry.message.timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"))

            Text(
                text       = "✓✓  today at $time",
                fontFamily = PersonaFont,
                fontSize   = 12.sp,
                color      = Color(0xFF888888),
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Card
// ---------------------------------------------------------------------------

@Composable
private fun MenuCard(
    modifier:  Modifier = Modifier,
    content:   @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val shape   = GenericShape { size, _ ->
        val skew = with(density) { 6.dp.toPx() }
        moveTo(skew, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width - skew, size.height)
        lineTo(0f, size.height)
        close()
    }

    Column(
        modifier = modifier
            .widthIn(min = 240.dp, max = 300.dp)
            // Stop tap propagation so clicking card doesn't dismiss
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = {},
            )
            .clip(shape)
            .background(Color(0xFF1A1A1A)),
    ) {
        content()
    }
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
            .padding(vertical = 10.dp, horizontal = 8.dp),
    ) {
        QuickReactions.forEach { emoji ->
            Text(
                text     = emoji,
                fontSize = 28.sp,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { onReact(emoji) },
                    ),
            )
        }

        // Expand button
        Text(
            text       = "▼",
            fontSize   = 16.sp,
            color      = Color(0xFF888888),
            modifier   = Modifier
                .size(44.dp)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onExpand,
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// Action item row
// ---------------------------------------------------------------------------

@Composable
private fun ActionItem(action: MessageAction, onClick: () -> Unit) {
    val isDelete = action == MessageAction.Delete
    val color    = if (isDelete) Color(0xFFE53935) else Color.White

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text       = action.icon,
            fontSize   = 18.sp,
            color      = color,
        )
        Text(
            text       = action.label,
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Normal,
            fontSize   = 16.sp,
            color      = color,
        )
    }
}

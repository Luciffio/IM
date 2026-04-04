package com.example.im.ui.persona

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    QuickReply (Icons.AutoMirrored.Filled.Reply,    "Reply"),
    QuickEdit  (Icons.Default.Edit,        "Edit"),
    PinMessage (Icons.Default.PushPin,     "Pin"),
    CopyText   (Icons.Default.ContentCopy, "Copy"),
    ShareMsg   (Icons.AutoMirrored.Filled.Forward,  "Forward"),
    SelectMsg  (Icons.Default.CheckBox,    "Select"),
}

private val QuickReactions = listOf("👍", "❤️", "🔥", "😂", "😮", "👌", "😍")

// Permanent P5 lean applied to the resting card
private const val P5_TILT = 2f

// ---------------------------------------------------------------------------
// Overlay
// ---------------------------------------------------------------------------

/**
 * Full-screen dimmed overlay hosting the P5 "Phantom" context menu.
 *
 * **Smart positioning:**
 * - Message in top half of screen  → card appears *below* the bubble, tail at TOP
 * - Message in bottom half          → card appears *above* the bubble, tail at BOTTOM
 *
 * The card grows from scale 0 → 1 with the tail tip as the transform origin,
 * so it visually "bursts" out of the long-pressed bubble.
 */
@Composable
fun PersonaMessageMenu(
    entry:         PersonaEntry,
    anchorOffset:  Offset       = Offset.Zero,
    isOutgoing:    Boolean      = false,
    onDismiss:     () -> Unit,
    onReact:       (String) -> Unit,
    onAction:      (MessageAction) -> Unit,
    onExpandEmoji: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val density   = LocalDensity.current

    val actions: List<MessageAction> = buildList {
        add(MessageAction.QuickReply)
        if (entry.message.isOutgoing) add(MessageAction.QuickEdit)
        add(MessageAction.PinMessage)
        add(MessageAction.CopyText)
        add(MessageAction.ShareMsg)
        add(MessageAction.SelectMsg)
    }

    val timestamp = LocalDateTime
        .ofInstant(Instant.ofEpochMilli(entry.message.timestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    var highlighted by remember { mutableStateOf<MessageAction?>(actions.firstOrNull()) }

    // ── Tail position depends on message side ────────────────────────────────
    // Outgoing (my msg, right side)  → tail on RIGHT edge of card
    // Incoming (other's msg, left)   → tail on LEFT edge of card
    val nomCardW     = 252.dp
    val tailEdgeGap  = 40.dp          // tail center distance from its nearest card edge
    val tailCenterDp = if (isOutgoing) nomCardW - tailEdgeGap else tailEdgeGap

    // ── Entrance animation ───────────────────────────────────────────────────
    // Tilt leans TOWARD the message: incoming = lean right (+), outgoing = lean left (−)
    var appeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (appeared) 1f else 0f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label         = "menuScale",
    )
    val restingTilt = if (isOutgoing) -P5_TILT else P5_TILT
    val tilt by animateFloatAsState(
        targetValue   = if (appeared) restingTilt else restingTilt - 8f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label         = "menuTilt",
    )
    LaunchedEffect(Unit) { appeared = true }

    // ── Card size (measured after first layout while scale = 0) ─────────────
    var cardH by remember { mutableStateOf(340.dp) }

    // Screen dimensions via LocalConfiguration — avoids BoxWithConstraints scope issues
    val config      = LocalConfiguration.current
    val screenW: Dp = config.screenWidthDp.dp
    val screenH: Dp = config.screenHeightDp.dp
    val screenHPx   = with(density) { screenH.toPx() }

    // Screen-space anchor in dp
    val anchorXDp = with(density) { anchorOffset.x.toDp() }
    val anchorYDp = with(density) { anchorOffset.y.toDp() }

    // Tail is at TOP when message is in the top half → card appears below
    val tailAtTop = anchorOffset.y < screenHPx / 2

    // Horizontal: align tail centre with anchor X, clamp inside screen
    val cardLeft: Dp = (anchorXDp - tailCenterDp)
        .coerceIn(8.dp, screenW - nomCardW - 8.dp)

    // Vertical: place card fully above or below the anchor
    val gap = 6.dp
    val cardTop: Dp = if (tailAtTop) {
        (anchorYDp + gap).coerceIn(4.dp, screenH - cardH - 4.dp)
    } else {
        (anchorYDp - cardH - gap).coerceIn(4.dp, screenH - cardH - 4.dp)
    }

    // TransformOrigin: tail tip at top or bottom of card (Dp/Dp → Float directly)
    val originX = (tailCenterDp / nomCardW).coerceIn(0f, 1f)
    val originY = if (tailAtTop) 0f else 1f

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
        Box(
            contentAlignment = Alignment.TopStart,
            modifier         = Modifier.fillMaxSize(),
        ) {
            PhantomCard(
                tailAtTop     = tailAtTop,
                tailCenterDp  = tailCenterDp,
                modifier      = Modifier
                    .offset(x = cardLeft, y = cardTop)
                    .onSizeChanged { px ->
                        cardH = with(density) { px.height.toDp() }
                    }
                    .graphicsLayer {
                        scaleX          = scale
                        scaleY          = scale
                        rotationZ       = tilt
                        transformOrigin = TransformOrigin(originX, originY)
                    },
            ) {
                // ── Reaction row ─────────────────────────────────────────
                ReactionsRow(
                    onReact = { emoji -> onReact(emoji); onDismiss() },
                )

                HorizontalDivider(color = Color(0xFFCCCCCC), thickness = 0.5.dp)

                // ── Action list ───────────────────────────────────────────
                actions.forEachIndexed { idx, action ->
                    ActionRow(
                        label      = action.label,
                        icon       = action.icon,
                        isSelected = action == highlighted,
                        isAlert    = false,
                        onClick    = {
                            highlighted = action
                            if (action == MessageAction.CopyText) {
                                clipboard.setText(AnnotatedString(entry.message.text))
                            }
                            onAction(action)
                            onDismiss()
                        },
                    )
                    if (idx < actions.lastIndex) {
                        HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 0.5.dp)
                    }
                }

                HorizontalDivider(color = Color(0xFFCCCCCC), thickness = 0.5.dp)

                ActionRow(
                    label      = "Remove",
                    icon       = Icons.Default.Delete,
                    isSelected = false,
                    isAlert    = true,
                    onClick    = { onDismiss() },
                )

                // ── Timestamp ─────────────────────────────────────────────
                Text(
                    text       = "✓✓  $timestamp",
                    fontFamily = PersonaFont,
                    fontSize   = 10.sp,
                    color      = Color(0xFF999999),
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Card shell — drawBehind handles white fill + 3 dp black border; no clip()
// ---------------------------------------------------------------------------

private const val CARD_BORDER_DP = 3f

@Composable
private fun PhantomCard(
    tailAtTop:    Boolean,
    tailCenterDp: Dp      = MENU_TAIL_CENTER_X_DP.dp,
    modifier:     Modifier = Modifier,
    content:      @Composable () -> Unit,
) {
    val density  = LocalDensity.current
    val tailPad  = MENU_TAIL_H_DP.dp

    Box(
        modifier = modifier
            .widthIn(min = 240.dp, max = 260.dp)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = {},
            )
            .drawBehind {
                val tailCenterPx = with(density) { tailCenterDp.toPx() }
                val path = with(density) {
                    buildPhantomMenuPath(size.width, size.height, tailAtTop, tailCenterPx)
                }
                drawPath(path, Color.White)          // white fill
                drawPath(                            // 3 dp black border — miter joins for sharp P5 corners
                    path  = path,
                    color = Color.Black,
                    style = Stroke(
                        width = CARD_BORDER_DP.dp.toPx(),
                        join  = StrokeJoin.Miter,
                        miter = 10f,
                    ),
                )
            },
    ) {
        Column(
            modifier = Modifier.padding(
                top    = if (tailAtTop) tailPad else 0.dp,
                bottom = if (tailAtTop) 0.dp    else tailPad,
            ),
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Reactions row — plain emoji, no tiles
// ---------------------------------------------------------------------------

@Composable
private fun ReactionsRow(onReact: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        QuickReactions.forEach { emoji ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { onReact(emoji) },
                    ),
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Action row — P5 chromatic-aberration selection highlight
// ---------------------------------------------------------------------------

// Aberration offsets: just 2 px — very subtle, like the reference
private val CHROMA_PINK   = Color(0xFFFF00AA)
private val CHROMA_BLUE   = Color(0xFF0055FF)

@Composable
private fun ActionRow(
    label:      String,
    icon:       ImageVector,
    isSelected: Boolean,
    isAlert:    Boolean,
    onClick:    () -> Unit,
) {
    val textColor = when {
        isSelected -> Color.White
        isAlert    -> Color(0xFFE53935)
        else       -> Color(0xFF111111)
    }
    val iconColor = when {
        isSelected -> Color.White
        isAlert    -> Color(0xFFE53935)
        else       -> Color(0xFF444444)
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .then(
                if (isSelected) Modifier.drawBehind {
                    // Chromatic aberration: pink shifted 2 px left, blue shifted 2 px right
                    // Then the main black layer covers almost all of them — only ~2 px fringe visible
                    val aberrationPx = 2.dp.toPx()
                    drawRect(CHROMA_PINK, topLeft = Offset(-aberrationPx, 0f), size = size)
                    drawRect(CHROMA_BLUE, topLeft = Offset( aberrationPx, 0f), size = size)
                    drawRect(Color.Black)   // black selection band on top
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = iconColor,
            modifier           = Modifier.size(18.dp),
        )
        Text(
            text       = label,
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize   = 14.sp,
            color      = textColor,
        )
    }
}

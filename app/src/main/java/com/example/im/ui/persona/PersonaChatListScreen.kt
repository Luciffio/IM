package com.example.im.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.clipToBounds
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun PersonaChatListScreen(
    chats:        List<ChatPreview> = SampleChatList,
    onChatClick:  (ChatPreview) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PersonaRed),
    ) {
        ChatListHeader()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
        ) {
            items(chats, key = { it.id }) { chat ->
                ChatItem(
                    chat    = chat,
                    onClick = { onChatClick(chat) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header — "IM" logo + date
// ---------------------------------------------------------------------------

@Composable
private fun ChatListHeader() {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(72.dp),
    ) {
        // Black diagonal banner behind the logo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.55f, 0f)
                        lineTo(size.width * 0.45f, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, Color.Black)
                }
        ) {}

        // "IM" text
        Text(
            text       = "IM",
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize   = 42.sp,
            color      = Color.White,
            modifier   = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp, bottom = 4.dp),
        )

        // Red accent stripe on "I"
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 20.dp, y = (-2).dp)
                .size(width = 10.dp, height = 6.dp)
                .background(PersonaRed),
        ) {}
    }
}

// ---------------------------------------------------------------------------
// Chat item
// ---------------------------------------------------------------------------

private val ITEM_HEIGHT      = 72.dp
private val BADGE_WIDTH      = 90.dp
private val BADGE_HEIGHT     = 28.dp
private val AVATAR_SIZE      = 54.dp
private val STRIP_LEFT_PAD   = 36.dp   // how much strip starts from left edge
private val STRIP_ARROW      = 20.dp   // depth of right-pointing arrow tip
private val STRIP_RIGHT_PAD  = 32.dp   // gap from screen right edge

@Composable
private fun ChatItem(chat: ChatPreview, onClick: () -> Unit) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT + 12.dp)   // extra space for badge overhang
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            ),
    ) {
        // ── Main strip ──────────────────────────────────────────────────────
        val stripColor = if (chat.hasUnread) PersonaRed else Color.Black

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .padding(start = STRIP_LEFT_PAD, end = STRIP_RIGHT_PAD)
                .drawWithCache {
                    val arrowPx  = with(density) { STRIP_ARROW.toPx() }

                    // Outer black border shape
                    val outerShape = GenericShape { s, _ ->
                        val border = with(density) { 3.dp.toPx() }
                        moveTo(-border, -border)
                        lineTo(s.width - arrowPx + border, -border)
                        lineTo(s.width + border, s.height / 2f)
                        lineTo(s.width - arrowPx + border, s.height + border)
                        lineTo(-border, s.height + border)
                        close()
                    }
                    val outerOutline = outerShape.createOutline(size, layoutDirection, this)

                    // Inner colored strip
                    val innerShape = GenericShape { s, _ ->
                        moveTo(0f, 0f)
                        lineTo(s.width - arrowPx, 0f)
                        lineTo(s.width, s.height / 2f)
                        lineTo(s.width - arrowPx, s.height)
                        lineTo(0f, s.height)
                        close()
                    }
                    val innerOutline = innerShape.createOutline(size, layoutDirection, this)

                    onDrawBehind {
                        drawOutline(outerOutline, Color.Black)
                        drawOutline(innerOutline, stripColor)
                    }
                },
        ) {
            // Message text inside strip
            Text(
                text       = chat.lastMessage,
                fontFamily = PersonaFont,
                fontWeight = if (chat.hasUnread) FontWeight.Black else FontWeight.Normal,
                fontSize   = 16.sp,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = AVATAR_SIZE + 4.dp, end = STRIP_ARROW + 8.dp),
            )
        }

        // ── Avatar ──────────────────────────────────────────────────────────
        ChatAvatar(
            chat     = chat,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 8.dp, y = (-8).dp)
                .size(AVATAR_SIZE),
        )

        // ── Date badge ──────────────────────────────────────────────────────
        ChatDateBadge(
            date     = chat.date,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 40.dp, y = 0.dp)
                .size(width = BADGE_WIDTH, height = BADGE_HEIGHT),
        )
    }
}

// ---------------------------------------------------------------------------
// Date badge
// ---------------------------------------------------------------------------

@Composable
private fun ChatDateBadge(date: LocalDate, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val month   = date.monthValue.toString()
    val day     = date.dayOfMonth.toString()
    val weekday = date.format(DateTimeFormatter.ofPattern("EEE")).take(2)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.drawWithCache {
            val skew = with(density) { 5.dp.toPx() }

            val outerShape = GenericShape { s, _ ->
                moveTo(skew, 0f)
                lineTo(s.width, 0f)
                lineTo(s.width - skew, s.height)
                lineTo(0f, s.height)
                close()
            }
            val outerOutline = outerShape.createOutline(size, layoutDirection, this)

            val pad = with(density) { 2.dp.toPx() }
            val innerShape = GenericShape { s, _ ->
                moveTo(skew + pad, pad)
                lineTo(s.width - pad, pad)
                lineTo(s.width - skew - pad, s.height - pad)
                lineTo(pad, s.height - pad)
                close()
            }
            val innerOutline = innerShape.createOutline(size, layoutDirection, this)

            onDrawBehind {
                drawOutline(outerOutline, Color.Black)
                drawOutline(innerOutline, Color.White)
            }
        },
    ) {
        // "3|3 Fr" — day number is largest
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(month,   fontFamily = PersonaFont, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black,
                modifier = Modifier.alignByBaseline())
            Text("|",     fontFamily = PersonaFont, fontSize = 11.sp, color = Color(0xFF555555),
                modifier = Modifier.padding(horizontal = 2.dp).alignByBaseline())
            Text(day,     fontFamily = PersonaFont, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black,
                modifier = Modifier.alignByBaseline())
            Text(" $weekday", fontFamily = PersonaFont, fontSize = 11.sp, color = Color.Black,
                modifier = Modifier.alignByBaseline())
        }
    }
}

// ---------------------------------------------------------------------------
// Avatar
// ---------------------------------------------------------------------------

@Composable
private fun ChatAvatar(chat: ChatPreview, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    // Clip to the same parallelogram as the TranscriptState avatar shape,
    // scaled to the smaller size used in the list.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .drawBehind {
                // Black outer box
                val blackShape = GenericShape { s, _ ->
                    moveTo(0f,      s.height * 0.18f)
                    lineTo(s.width * 0.91f, s.height * 0.3f)
                    lineTo(s.width, s.height * 0.81f)
                    lineTo(s.width * 0.3f,  s.height)
                    close()
                }
                drawOutline(blackShape.createOutline(size, layoutDirection, this), Color.Black)

                // White box
                val whiteShape = GenericShape { s, _ ->
                    moveTo(s.width * 0.15f, s.height * 0.23f)
                    lineTo(s.width * 0.88f, s.height * 0.34f)
                    lineTo(s.width * 0.97f, s.height * 0.78f)
                    lineTo(s.width * 0.34f, s.height * 0.89f)
                    close()
                }
                drawOutline(whiteShape.createOutline(size, layoutDirection, this), Color.White)

                // Colored accent box
                val colorShape = GenericShape { s, _ ->
                    moveTo(s.width * 0.20f, s.height * 0.31f)
                    lineTo(s.width * 0.86f, s.height * 0.35f)
                    lineTo(s.width * 0.95f, s.height * 0.75f)
                    lineTo(s.width * 0.36f, s.height * 0.85f)
                    close()
                }
                drawOutline(colorShape.createOutline(size, layoutDirection, this), chat.color)
            },
    ) {
        // Initial letter as placeholder (replace with real avatar image later)
        Text(
            text       = chat.name.first().toString(),
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize   = 20.sp,
            color      = Color.Black,
        )
    }
}

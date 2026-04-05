package com.example.im.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════════════════
// Screen
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PersonaChatListScreen(
    chats:           List<ChatPreview>     = SampleChatList,
    onChatClick:     (ChatPreview) -> Unit = {},
    onSettingsClick: () -> Unit            = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PersonaRed),
    ) {
        // ── Main content ─────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            ChatListHeader(onSearchClick = { /* TODO: open search */ })

            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 72.dp),
            ) {
                items(chats, key = { it.id }) { chat ->
                    ChatItem(chat = chat, onClick = { onChatClick(chat) })
                }
            }
        }

        // ── Add-chat button (placeholder) ────────────────────────────────────
        AddChatButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 68.dp),
        )

        // ── Bottom navigation bar overlay ────────────────────────────────────
        ChatListNavBar(
            onSettingsClick = onSettingsClick,
            modifier        = Modifier.align(Alignment.BottomStart),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Header — "IM" badge logo + search icon
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatListHeader(onSearchClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(68.dp),
    ) {
        // ── IM logo: rotated black badge with white border ────────────────────
        // Reference: P5 logo badge — black square, ~-8° tilt, white frame,
        // "I" inside a white box outline, "M" plain bold.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .rotate(-8f)
                .size(54.dp)
                .drawBehind {
                    val bw = 3.dp.toPx()
                    // White outer border
                    drawRect(Color.White)
                    // Black inner fill
                    drawRect(
                        color   = Color.Black,
                        topLeft = Offset(bw, bw),
                        size    = Size(size.width - bw * 2f, size.height - bw * 2f),
                    )
                },
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // "I" — with white stroke box around it (P5 logo treatment)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .drawBehind {
                            drawRect(
                                color = Color.White,
                                style = Stroke(width = 1.8.dp.toPx()),
                            )
                        }
                        .padding(horizontal = 2.dp, vertical = 0.dp),
                ) {
                    Text(
                        text       = "I",
                        fontFamily = PersonaFont,
                        fontWeight = FontWeight.Black,
                        fontSize   = 24.sp,
                        color      = Color.White,
                    )
                }
                Text(
                    text       = "M",
                    fontFamily = PersonaFont,
                    fontWeight = FontWeight.Black,
                    fontSize   = 24.sp,
                    color      = Color.White,
                    modifier   = Modifier.padding(start = 1.dp),
                )
            }
        }

        // ── Search button (top-right) ─────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(40.dp)
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onSearchClick,
                ),
        ) {
            Icon(
                imageVector        = Icons.Default.Search,
                contentDescription = "Search",
                tint               = Color.White,
                modifier           = Modifier.size(24.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Chat item — slimmed-down from original 72 dp → 56 dp
// ═══════════════════════════════════════════════════════════════════════════════

private val ITEM_HEIGHT     = 56.dp
private val BADGE_WIDTH     = 76.dp
private val BADGE_HEIGHT    = 22.dp
private val AVATAR_SIZE     = 44.dp
private val STRIP_LEFT_PAD  = 28.dp
private val STRIP_ARROW     = 16.dp
private val STRIP_RIGHT_PAD = 24.dp

@Composable
private fun ChatItem(chat: ChatPreview, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ITEM_HEIGHT + 10.dp)   // 10 dp overhang for date badge
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
                    val arrowPx = STRIP_ARROW.toPx()
                    val bPx     = 3.dp.toPx()

                    val outerShape = GenericShape { s, _ ->
                        moveTo(-bPx, -bPx)
                        lineTo(s.width - arrowPx + bPx, -bPx)
                        lineTo(s.width + bPx, s.height / 2f)
                        lineTo(s.width - arrowPx + bPx, s.height + bPx)
                        lineTo(-bPx, s.height + bPx)
                        close()
                    }
                    val innerShape = GenericShape { s, _ ->
                        moveTo(0f, 0f)
                        lineTo(s.width - arrowPx, 0f)
                        lineTo(s.width, s.height / 2f)
                        lineTo(s.width - arrowPx, s.height)
                        lineTo(0f, s.height)
                        close()
                    }
                    val outerOutline = outerShape.createOutline(size, layoutDirection, this)
                    val innerOutline = innerShape.createOutline(size, layoutDirection, this)

                    onDrawBehind {
                        drawOutline(outerOutline, Color.Black)
                        drawOutline(innerOutline, stripColor)
                    }
                },
        ) {
            // Contact name (small, top of strip)
            Text(
                text       = chat.name.uppercase(),
                fontFamily = PersonaFont,
                fontWeight = FontWeight.Black,
                fontSize   = 9.sp,
                color      = Color.White.copy(alpha = 0.55f),
                letterSpacing = 1.sp,
                maxLines   = 1,
                modifier   = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = AVATAR_SIZE + 6.dp, top = 5.dp, end = STRIP_ARROW + 4.dp),
            )
            // Last message (main line)
            Text(
                text       = chat.lastMessage,
                fontFamily = PersonaFont,
                fontWeight = if (chat.hasUnread) FontWeight.Black else FontWeight.Normal,
                fontSize   = 13.sp,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = AVATAR_SIZE + 6.dp, bottom = 8.dp, end = STRIP_ARROW + 6.dp),
            )
        }

        // ── Avatar ──────────────────────────────────────────────────────────
        ChatAvatar(
            chat     = chat,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 6.dp, y = (-6).dp)
                .size(AVATAR_SIZE),
        )

        // ── Date badge ──────────────────────────────────────────────────────
        ChatDateBadge(
            date     = chat.date,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 32.dp, y = 0.dp)
                .size(width = BADGE_WIDTH, height = BADGE_HEIGHT),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Date badge
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatDateBadge(date: LocalDate, modifier: Modifier = Modifier) {
    val month   = date.monthValue.toString()
    val day     = date.dayOfMonth.toString()
    val weekday = date.format(DateTimeFormatter.ofPattern("EEE")).take(2)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.drawWithCache {
            val skewPx = 5.dp.toPx()
            val padPx  = 2.dp.toPx()

            val outer = GenericShape { s, _ ->
                moveTo(skewPx, 0f); lineTo(s.width, 0f)
                lineTo(s.width - skewPx, s.height); lineTo(0f, s.height); close()
            }
            val inner = GenericShape { s, _ ->
                moveTo(skewPx + padPx, padPx); lineTo(s.width - padPx, padPx)
                lineTo(s.width - skewPx - padPx, s.height - padPx)
                lineTo(padPx, s.height - padPx); close()
            }
            val outerOutline = outer.createOutline(size, layoutDirection, this)
            val innerOutline = inner.createOutline(size, layoutDirection, this)

            onDrawBehind {
                drawOutline(outerOutline, Color.Black)
                drawOutline(innerOutline, Color.White)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(month,      fontFamily = PersonaFont, fontSize = 9.sp,  fontWeight = FontWeight.Black, color = Color.Black,
                modifier = Modifier.alignByBaseline())
            Text("|",        fontFamily = PersonaFont, fontSize = 8.sp,  color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 1.dp).alignByBaseline())
            Text(day,        fontFamily = PersonaFont, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black,
                modifier = Modifier.alignByBaseline())
            Text(" $weekday",fontFamily = PersonaFont, fontSize = 9.sp,  color = Color.Black,
                modifier = Modifier.alignByBaseline())
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Avatar
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatAvatar(chat: ChatPreview, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.drawBehind {
            val black = GenericShape { s, _ ->
                moveTo(0f, s.height * 0.18f); lineTo(s.width * 0.91f, s.height * 0.30f)
                lineTo(s.width, s.height * 0.81f); lineTo(s.width * 0.30f, s.height); close()
            }
            drawOutline(black.createOutline(size, layoutDirection, this), Color.Black)

            val white = GenericShape { s, _ ->
                moveTo(s.width * 0.15f, s.height * 0.23f); lineTo(s.width * 0.88f, s.height * 0.34f)
                lineTo(s.width * 0.97f, s.height * 0.78f); lineTo(s.width * 0.34f, s.height * 0.89f); close()
            }
            drawOutline(white.createOutline(size, layoutDirection, this), Color.White)

            val color = GenericShape { s, _ ->
                moveTo(s.width * 0.20f, s.height * 0.31f); lineTo(s.width * 0.86f, s.height * 0.35f)
                lineTo(s.width * 0.95f, s.height * 0.75f); lineTo(s.width * 0.36f, s.height * 0.85f); close()
            }
            drawOutline(color.createOutline(size, layoutDirection, this), chat.color)
        },
    ) {
        Text(
            text       = chat.name.first().toString(),
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize   = 16.sp,
            color      = Color.Black,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Add-chat button — P5 parallelogram with "+" (placeholder)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddChatButton(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 50.dp, height = 42.dp)
            .drawBehind {
                val skew = size.height * 0.18f
                val bw   = 2.5.dp.toPx()

                val outer = Path().apply {
                    moveTo(skew, 0f); lineTo(size.width, 0f)
                    lineTo(size.width - skew, size.height); lineTo(0f, size.height); close()
                }
                val inner = Path().apply {
                    moveTo(skew + bw, bw); lineTo(size.width - bw, bw)
                    lineTo(size.width - skew - bw, size.height - bw)
                    lineTo(bw, size.height - bw); close()
                }
                drawPath(outer, Color.White)
                drawPath(inner, Color.Black)
            }
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = { /* TODO: new chat / group dialog */ },
            ),
    ) {
        Text(
            text       = "+",
            fontFamily = PersonaFont,
            fontWeight = FontWeight.Black,
            fontSize   = 24.sp,
            color      = Color.White,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Bottom navigation bar — same diagonal direction as TopBar
// Shape: top-left is raised (consistent with P5 lean direction)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatListNavBar(onSettingsClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    // Outer Column has a solid black background that extends ALL the way to the
    // bottom edge of the screen, covering the system navigation bar area so no
    // red "gap" is visible below the angled bar.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
    ) {
        // Diagonal 54 dp visible bar — clipped top edge, icons inside
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(GenericShape { size, _ ->
                    val raise = size.height * 0.26f   // ≈14 dp at 54 dp height
                    moveTo(0f, 0f)                    // top-left (raised)
                    lineTo(size.width, raise)          // top-right (lower)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                })
                .background(Color.Black),
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                NavBarItem(icon = Icons.Default.Home,   label = "Chats",    active = true)
                NavBarItem(icon = Icons.Default.Person, label = "Contacts", active = false)
                SettingsNavBarItem(active = false, onClick = onSettingsClick)
            }
        }

        // Absorb system navigation bar insets — Spacer height = nav bar height,
        // background inherited from outer Column (black), so no gap shows.
        Spacer(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
    }
}

@Composable
private fun NavBarItem(
    icon:    ImageVector,
    label:   String,
    active:  Boolean,
    onClick: () -> Unit = {},
) {
    val tint = if (active) PersonaRed else Color.White.copy(alpha = 0.55f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = tint,
            modifier           = Modifier.size(20.dp),
        )
        Text(
            text       = label,
            fontFamily = PersonaFont,
            fontSize   = 8.sp,
            color      = tint,
            fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
        )
    }
}

@Composable
private fun SettingsNavBarItem(active: Boolean, onClick: () -> Unit = {}) {
    val tint = if (active) PersonaRed else Color.White.copy(alpha = 0.55f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        GearIcon(tint = tint)
        Text(
            text       = "Settings",
            fontFamily = PersonaFont,
            fontSize   = 8.sp,
            color      = tint,
            fontWeight = if (active) FontWeight.Black else FontWeight.Normal,
        )
    }
}

@Composable
private fun GearIcon(tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .drawWithCache {
                val cx     = size.width / 2f
                val cy     = size.height / 2f
                val outer  = size.width * 0.46f
                val inner  = size.width * 0.30f
                val holeR  = size.width * 0.15f
                val teeth  = 8

                val path = androidx.compose.ui.graphics.Path()
                for (i in 0 until teeth * 2) {
                    val angle = (i * Math.PI / teeth - Math.PI / 2).toFloat()
                    val r = if (i % 2 == 0) outer else inner
                    val x = cx + r * kotlin.math.cos(angle)
                    val y = cy + r * kotlin.math.sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                onDrawBehind {
                    drawPath(path, tint)
                    drawCircle(Color.Black, holeR, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
            },
    ) {}
}

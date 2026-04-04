package com.example.im.ui.persona

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun PersonaChatScreen(
    state:          PersonaChatState = rememberPersonaChatState(),
    onBack:         () -> Unit       = {},
    date:           LocalDate        = LocalDate.now(),
    contactName:    String           = "PHANTOM THIEVES",
    contactColor:   Color            = P5ColorAnn,
    contactInitial: String           = "P",
) {
    var showEmojiPanel  by remember { mutableStateOf(false) }
    var inputText       by remember { mutableStateOf("") }
    var selectedEntry   by remember { mutableStateOf<PersonaEntry?>(null) }
    // messageId -> (emoji -> count)
    val reactions = remember { mutableStateMapOf<Long, MutableMap<String, Int>>() }
    val keyboard = LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Content column (no TopBar here — it's an overlay below) ─────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PersonaRed)
                .imePadding(),
        ) {
            PersonaTranscript(
                entries   = state.entries,
                reactions = reactions,
                onLongPress = { entry ->
                    selectedEntry  = entry
                    showEmojiPanel = false
                    keyboard?.hide()
                },
                modifier = Modifier.weight(1f),
            )

            PersonaInputBar(
                text           = inputText,
                onTextChange   = { inputText = it },
                showEmojiPanel = showEmojiPanel,
                onEmojiToggle  = {
                    showEmojiPanel = !showEmojiPanel
                    if (showEmojiPanel) keyboard?.hide()
                    else keyboard?.show()
                },
                onSend = {
                    state.sendMessage(inputText)
                    inputText      = ""
                    showEmojiPanel = false
                },
            )

            PersonaEmojiPanel(
                visible      = showEmojiPanel,
                onEmojiClick = { emoji -> inputText += emoji },
            )
        }

        // ── TopBar overlay — clips to trapezoid, draws on top of content ────
        // Content scrolls under the black area; the diagonal "cut" corner
        // correctly reveals the red chat background below it.
        PersonaTopBar(
            onBack         = onBack,
            date           = date,
            contactName    = contactName,
            contactColor   = contactColor,
            contactInitial = contactInitial,
        )

        // ── Context menu overlay ─────────────────────────────────────────────
        selectedEntry?.let { entry ->
            PersonaMessageMenu(
                entry        = entry,
                onDismiss    = { selectedEntry = null },
                onReact      = { emoji ->
                    val map = reactions.getOrPut(entry.message.id) { mutableMapOf() }
                    map[emoji] = (map[emoji] ?: 0) + 1
                },
                onAction     = { /* TODO: implement actions */ },
                onExpandEmoji = { showEmojiPanel = true },
            )
        }
    }
}

// ── Input bar ────────────────────────────────────────────────────────────────

@Composable
private fun PersonaInputBar(
    text:           String,
    onTextChange:   (String) -> Unit,
    showEmojiPanel: Boolean,
    onEmojiToggle:  () -> Unit,
    onSend:         () -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // [📎] Paperclip attachment button
        AttachButton(onClick = { /* TODO: open file picker */ })

        // Text field
        BasicTextField(
            value         = text,
            onValueChange = onTextChange,
            singleLine    = true,
            textStyle     = TextStyle(
                fontFamily = PersonaFont,
                fontSize   = 15.sp,
                color      = Color.White,
            ),
            cursorBrush     = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
            decorationBox   = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text       = "Enter message...",
                            fontFamily = PersonaFont,
                            fontSize   = 15.sp,
                            color      = Color(0xFF555555),
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f),
        )

        // [😊] Emoji toggle — right of input, left of Send
        EmojiButton(
            active  = showEmojiPanel,
            onClick = onEmojiToggle,
        )

        // [Send]
        SendButton(
            enabled = text.isNotBlank(),
            onClick = onSend,
        )
    }
}

// ── Attach button (📎 paperclip) ──────────────────────────────────────────────
//
// Standard Telegram-style AttachFile icon — no custom styling.

@Composable
private fun AttachButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            ),
    ) {
        Icon(
            imageVector        = Icons.Default.AttachFile,
            contentDescription = "Attach file",
            tint               = Color.White,
            modifier           = Modifier.size(24.dp),
        )
    }
}

// ── Emoji toggle button ───────────────────────────────────────────────────────

@Composable
private fun EmojiButton(active: Boolean, onClick: () -> Unit) {
    val density  = LocalDensity.current
    // On black bar: inactive = white icon; active = black icon on white bg
    val fgColor  = if (active) Color.Black else Color.White
    val bgColor  = if (active) Color.White else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .drawWithCache {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r  = size.width * 0.36f

                // Simple smiley face path drawn manually for P5 feel
                val facePath = Path().apply {
                    // outer circle approximated as octagon
                    val pts = 8
                    for (i in 0 until pts) {
                        val angle = Math.toRadians((i * 360.0 / pts) - 90)
                        val x = cx + r * Math.cos(angle).toFloat()
                        val y = cy + r * Math.sin(angle).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                onDrawBehind {
                    if (active) drawPath(facePath, Color.Black)
                    // Eyes
                    val eyeR = r * 0.13f
                    drawCircle(fgColor, eyeR, androidx.compose.ui.geometry.Offset(cx - r * 0.3f, cy - r * 0.2f))
                    drawCircle(fgColor, eyeR, androidx.compose.ui.geometry.Offset(cx + r * 0.3f, cy - r * 0.2f))
                    // Smile arc approximated with a path
                    val smilePath = Path().apply {
                        moveTo(cx - r * 0.35f, cy + r * 0.1f)
                        quadraticTo(cx, cy + r * 0.55f, cx + r * 0.35f, cy + r * 0.1f)
                    }
                    drawPath(smilePath, fgColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 2.dp.toPx() }))
                }
            }
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            ),
    ) {}
}

// ── Send button — P5 octagon style (ref: Persona action buttons) ──────────────
//
// Layer 1 (back) : white filled octagon  (flat edges at top/bottom/left/right)
// Layer 2        : black filled circle   (~73 % of octagon radius)
// Layer 3 (front): white equilateral triangle pointing RIGHT (~54 % of circle)
//
// Disabled state: all white → dim-gray.

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val fgColor = if (enabled) Color.White else Color(0xFF555555)

    Box(
        modifier = Modifier
            .size(40.dp)
            .drawWithCache {
                val cx      = size.width  / 2f
                val cy      = size.height / 2f
                val outerR  = size.width  * 0.45f     // octagon circumradius
                val circleR = outerR * 0.855f          // black circle radius (~45% thinner ring)
                val triR    = circleR * 0.54f          // triangle circumradius

                // Stop-sign octagon: vertices at 22.5° + k·45° (screen coords, y-down)
                val octPath = Path().apply {
                    for (i in 0 until 8) {
                        val a = Math.toRadians(22.5 + i * 45.0)
                        val x = cx + outerR * Math.cos(a).toFloat()
                        val y = cy + outerR * Math.sin(a).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }

                // Right-pointing equilateral triangle
                // Vertices at 0°, 120°, 240° clockwise from the right
                val triPath = Path().apply {
                    moveTo(cx + triR,         cy)
                    lineTo(cx - triR * 0.5f,  cy + triR * 0.866f)
                    lineTo(cx - triR * 0.5f,  cy - triR * 0.866f)
                    close()
                }

                onDrawBehind {
                    drawPath(octPath, fgColor)          // 1. octagon
                    drawCircle(Color.Black, circleR)    // 2. circle
                    drawPath(triPath, fgColor)          // 3. triangle
                }
            }
            .then(
                if (enabled) Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onClick,
                ) else Modifier
            ),
    )
}

// ── Transcript ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonaTranscript(
    entries:     List<PersonaEntry>,
    reactions:   Map<Long, Map<String, Int>> = emptyMap(),
    onLongPress: (PersonaEntry) -> Unit      = {},
    modifier:    Modifier = Modifier,
) {
    val listState      = rememberLazyListState()
    val totalItemCount by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }

    LaunchedEffect(totalItemCount) {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@LaunchedEffect
        if (last.index == totalItemCount - 1) {
            listState.animateScrollBy(
                value         = last.size.toFloat() + listState.layoutInfo.afterContentPadding,
                animationSpec = tween(durationMillis = 280),
            )
        } else {
            listState.animateScrollToItem(totalItemCount - 1)
        }
    }

    LazyColumn(
        state               = listState,
        verticalArrangement = Arrangement.spacedBy(PersonaSizes.EntrySpacing),
        contentPadding      = WindowInsets.systemBars
            .add(WindowInsets(top = 100.dp, bottom = 100.dp))
            .asPaddingValues(),
        modifier            = modifier,
    ) {
        itemsIndexed(
            items = entries,
            key   = { _, entry -> entry.message.id },
        ) { index, entry ->
            val lineModifier = Modifier.drawConnectingLine(
                entry1 = entry,
                entry2 = entries.getOrNull(index + 1),
            )
            val longPressModifier = Modifier.combinedClickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = {},
                onLongClick       = { onLongPress(entry) },
            )

            // Box so reaction badge overlaps the bubble — no extra space
            Box {
                if (entry.message.isOutgoing) {
                    PersonaOutgoingMessage(entry = entry, modifier = lineModifier.then(longPressModifier))
                } else {
                    PersonaIncomingMessage(entry = entry, modifier = lineModifier.then(longPressModifier))
                }

                val msgReactions = reactions[entry.message.id]
                if (!msgReactions.isNullOrEmpty()) {
                    ReactionBadge(
                        reactions = msgReactions,
                        // Sticker overlaps the bubble's bottom edge by 6 dp.
                        modifier  = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-6).dp, y = (-6).dp),
                    )
                }
            }
        }
    }
}

// ── Reaction badge ────────────────────────────────────────────────────────────

// Fixed tilt angles for a "scattered stickers" look — alternates per badge index.
private val ReactionTilts = listOf(-2.5f, 1.8f, -3.2f, 2.0f, -1.5f, 2.8f)

@Composable
private fun ReactionBadge(
    reactions: Map<String, Int>,
    modifier:  Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier,
    ) {
        reactions.entries.forEachIndexed { index, (emoji, count) ->
            SingleReactionBadge(
                emoji   = emoji,
                count   = count,
                tiltDeg = ReactionTilts[index % ReactionTilts.size],
            )
        }
    }
}

/**
 * One reaction sticker: pure-black parallelogram, white 1-dp border drawn
 * BEFORE clip so it's visible at the outer edge, emoji in natural color,
 * white bold counter.
 */
@Composable
private fun SingleReactionBadge(
    emoji:   String,
    count:   Int,
    tiltDeg: Float,
) {
    val density    = LocalDensity.current
    val badgeShape = with(density) { reactionBadgeShape() }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .rotate(tiltDeg)
            // drawBehind executes BEFORE clip is applied → white stroke is unclipped
            // → visible as a 1-dp border around the parallelogram edge.
            .drawBehind {
                val outline = badgeShape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline = outline,
                    color   = Color.White,
                    style   = Stroke(width = with(density) { 1.5.dp.toPx() }),
                )
            }
            .clip(badgeShape)
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(emoji, fontSize = 13.sp)
        if (count > 1) {
            Text(
                text       = count.toString(),
                fontFamily = PersonaFont,
                fontWeight = FontWeight.Bold,
                fontSize   = 11.sp,
                color      = Color.White,
            )
        }
    }
}

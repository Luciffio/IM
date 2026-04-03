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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun PersonaChatScreen(
    state:  PersonaChatState = rememberPersonaChatState(),
    onBack: () -> Unit       = {},
    date:   LocalDate        = LocalDate.now(),
) {
    var showEmojiPanel by remember { mutableStateOf(false) }
    var inputText      by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PersonaRed)
            .imePadding(),
    ) {
        PersonaTopBar(onBack = onBack, date = date)

        PersonaTranscript(
            entries  = state.entries,
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
            .background(Color.White)
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // [+] Attachment button
        AttachButton(onClick = { /* TODO: open file picker */ })

        // Text field
        BasicTextField(
            value         = text,
            onValueChange = onTextChange,
            singleLine    = true,
            textStyle     = TextStyle(
                fontFamily = PersonaFont,
                fontSize   = 15.sp,
                color      = Color.Black,
            ),
            cursorBrush     = SolidColor(Color.Black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
            decorationBox   = { inner ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F0F0))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text       = "Enter message...",
                            fontFamily = PersonaFont,
                            fontSize   = 15.sp,
                            color      = Color(0xFF999999),
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

// ── Attach button (+) ────────────────────────────────────────────────────────

@Composable
private fun AttachButton(onClick: () -> Unit) {
    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .drawWithCache {
                val cx    = size.width / 2f
                val cy    = size.height / 2f
                val arm   = size.width * 0.28f   // half-length of each arm
                val thick = with(density) { 3.dp.toPx() }

                // P5 plus sign: two thick rectangles forming a cross
                val hPath = Path().apply {
                    moveTo(cx - arm, cy - thick / 2f)
                    lineTo(cx + arm, cy - thick / 2f)
                    lineTo(cx + arm, cy + thick / 2f)
                    lineTo(cx - arm, cy + thick / 2f)
                    close()
                }
                val vPath = Path().apply {
                    moveTo(cx - thick / 2f, cy - arm)
                    lineTo(cx + thick / 2f, cy - arm)
                    lineTo(cx + thick / 2f, cy + arm)
                    lineTo(cx - thick / 2f, cy + arm)
                    close()
                }

                onDrawBehind {
                    drawPath(hPath, Color.Black)
                    drawPath(vPath, Color.Black)
                }
            }
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            ),
    ) {}
}

// ── Emoji toggle button ───────────────────────────────────────────────────────

@Composable
private fun EmojiButton(active: Boolean, onClick: () -> Unit) {
    val density  = LocalDensity.current
    val fgColor  = if (active) Color.White else Color.Black
    val bgColor  = if (active) Color.Black else Color.Transparent

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

// ── Send button ───────────────────────────────────────────────────────────────

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val density    = LocalDensity.current
    val bgColor    = if (enabled) Color.Black else Color(0xFF888888)
    val innerColor = if (enabled) Color.White else Color(0xFFBBBBBB)

    val shape = GenericShape { size, _ ->
        val skew = with(density) { 6.dp.toPx() }
        moveTo(skew, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width - skew, size.height)
        lineTo(0f, size.height)
        close()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .drawBehind {
                val outerOutline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outerOutline, bgColor)
                val innerShape = GenericShape { s, _ ->
                    val skew = with(density) { 6.dp.toPx() }
                    val pad  = with(density) { 2.dp.toPx() }
                    moveTo(skew + pad, pad)
                    lineTo(s.width - pad, pad)
                    lineTo(s.width - skew - pad, s.height - pad)
                    lineTo(pad, s.height - pad)
                    close()
                }
                drawOutline(innerShape.createOutline(size, layoutDirection, this), innerColor)
            }
            .then(
                if (enabled) Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onClick,
                ) else Modifier
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text       = "Send",
            fontFamily = PersonaFont,
            fontSize   = 14.sp,
            color      = bgColor,
        )
    }
}

// ── Transcript ────────────────────────────────────────────────────────────────

@Composable
fun PersonaTranscript(
    entries:  List<PersonaEntry>,
    modifier: Modifier = Modifier,
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
            if (entry.message.isOutgoing) {
                PersonaOutgoingMessage(entry = entry, modifier = lineModifier)
            } else {
                PersonaIncomingMessage(entry = entry, modifier = lineModifier)
            }
        }
    }
}

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
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
            onSend = { state.sendMessage(it) },
        )
    }
}

// ── Input bar ────────────────────────────────────────────────────────────────

@Composable
private fun PersonaInputBar(
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    val doSend = {
        if (text.isNotBlank()) {
            onSend(text)
            text = ""
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(
                WindowInsets.navigationBars.asPaddingValues()
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Text field — fills available width
        BasicTextField(
            value         = text,
            onValueChange = { text = it },
            singleLine    = true,
            textStyle     = TextStyle(
                fontFamily = PersonaFont,
                fontSize   = 15.sp,
                color      = Color.Black,
            ),
            cursorBrush = SolidColor(Color.Black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { doSend() }),
            decorationBox = { inner ->
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

        // Send button — P5-styled parallelogram
        SendButton(
            enabled  = text.isNotBlank(),
            onClick  = { doSend() },
        )
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val density    = LocalDensity.current
    val bgColor    = if (enabled) Color.Black else Color(0xFF888888)
    val innerColor = if (enabled) Color.White else Color(0xFFBBBBBB)

    // P5-style skewed button shape (parallelogram)
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
                // Inner inset
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
                    indication          = null,
                    interactionSource   = remember { MutableInteractionSource() },
                    onClick             = onClick,
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

// ── Transcript ───────────────────────────────────────────────────────────────

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

package com.example.im.ui.persona

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Root screen. Wires together:
 *  - the scrollable transcript ([PersonaTranscript])
 *  - a FAB that calls [PersonaChatState.advance] to reveal the next message
 *  - the Persona 5 red-splatter background colour
 *
 * Call [PersonaChatState.advance] (or [PersonaChatState.advanceAll]) from any
 * trigger you like — button press, network callback, etc.
 */
@Composable
fun PersonaChatScreen(
    state: PersonaChatState = rememberPersonaChatState(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PersonaRed),
    ) {
        PersonaTranscript(entries = state.entries)

        FloatingActionButton(
            onClick          = { state.advance() },
            containerColor   = Color.Black,
            contentColor     = Color.White,
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Text(
                text       = "▶",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

/**
 * Scrollable list of all revealed messages.
 * Mirrors Transcript.kt from the reference — including the smooth animated scroll
 * that triggers whenever a new item is added to the list.
 */
@Composable
fun PersonaTranscript(entries: List<PersonaEntry>) {
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
        modifier            = Modifier.fillMaxSize(),
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

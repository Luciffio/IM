package com.example.im.ui.persona

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Placeholder content (replace with real Telegram data later)
// ---------------------------------------------------------------------------

private val PlaceholderEmoji = listOf(
    "😀","😂","🥹","😍","🤩","😎","🥸","🤯","😱","🫡",
    "🤔","😏","😒","🙄","😤","😡","🤬","💀","👻","🤖",
    "🐱","🐶","🦊","🐸","🐼","🐨","🦁","🐯","🦄","🐉",
    "🍕","🍔","🌮","🍜","🍣","🍩","🎂","☕","🧋","🍺",
    "⚽","🎮","🎸","🎯","🏆","🎲","🃏","🎭","🎪","🎨",
    "❤️","🧡","💛","💚","💙","💜","🖤","🤍","💯","✨",
)

private val PlaceholderStickers: List<Pair<Color, String>> = listOf(
    Color(0xFFFE93C9) to "Ann",
    Color(0xFFF0EA40) to "Ryuji",
    Color(0xFF1BC8F9) to "Yusuke",
    Color(0xFFFF6B6B) to "Makoto",
    Color(0xFF4ECDC4) to "Futaba",
    Color(0xFFFFE66D) to "Haru",
    Color(0xFFFF6B35) to "Morgana",
    Color(0xFF95E1D3) to "Akechi",
    Color(0xFFFE93C9) to "Ann 2",
    Color(0xFFF0EA40) to "Ryuji 2",
    Color(0xFF1BC8F9) to "Yusuke 2",
    Color(0xFFFF6B6B) to "Makoto 2",
)

private val PlaceholderGifLabels = List(12) { "GIF ${it + 1}" }

// ---------------------------------------------------------------------------
// Panel tabs
// ---------------------------------------------------------------------------

private enum class EmojiTab(val label: String, val icon: String) {
    Emoji("Emoji", "😊"),
    Stickers("Stickers", "🎭"),
    Gif("GIF", "GIF"),
}

// ---------------------------------------------------------------------------
// Main panel
// ---------------------------------------------------------------------------

/**
 * Slides up from the bottom when [visible] is true.
 * [onEmojiClick] is called with the selected emoji string.
 * Stickers and GIFs are placeholders — hook up to Telegram API later.
 */
@Composable
fun PersonaEmojiPanel(
    visible:      Boolean,
    onEmojiClick: (String) -> Unit,
    modifier:     Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically { it },
        exit    = slideOutVertically { it },
        modifier = modifier,
    ) {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = EmojiTab.entries

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding(),
        ) {
            // ── Tab row ──────────────────────────────────────────────────────
            PanelTabRow(
                tabs        = tabs,
                selectedIdx = selectedTab,
                onSelect    = { selectedTab = it },
            )

            // ── Content ──────────────────────────────────────────────────────
            Box(modifier = Modifier.height(260.dp)) {
                when (tabs[selectedTab]) {
                    EmojiTab.Emoji    -> EmojiGrid(onEmojiClick)
                    EmojiTab.Stickers -> StickerGrid()
                    EmojiTab.Gif      -> GifGrid()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab row
// ---------------------------------------------------------------------------

@Composable
private fun PanelTabRow(
    tabs:        List<EmojiTab>,
    selectedIdx: Int,
    onSelect:    (Int) -> Unit,
) {
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEachIndexed { i, tab ->
            val selected = i == selectedIdx
            val bgColor  = if (selected) Color.White else Color(0xFF333333)
            val txtColor = if (selected) Color.Black  else Color(0xFF999999)

            val shape = GenericShape { size, _ ->
                val skew = with(density) { 5.dp.toPx() }
                moveTo(skew, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - skew, size.height)
                lineTo(0f, size.height)
                close()
            }

            Text(
                text      = "${tab.icon}  ${tab.label}",
                color     = txtColor,
                fontSize  = 13.sp,
                fontFamily = PersonaFont,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .weight(1f)
                    .clip(shape)
                    .drawBehind { drawRect(bgColor) }
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { onSelect(i) },
                    )
                    .padding(vertical = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Emoji grid
// ---------------------------------------------------------------------------

@Composable
private fun EmojiGrid(onEmojiClick: (String) -> Unit) {
    LazyVerticalGrid(
        columns         = GridCells.Fixed(8),
        contentPadding  = PaddingValues(8.dp),
        verticalArrangement   = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(PlaceholderEmoji) { emoji ->
            Text(
                text      = emoji,
                fontSize  = 26.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .size(40.dp)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { onEmojiClick(emoji) },
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sticker grid (placeholder)
// ---------------------------------------------------------------------------

@Composable
private fun StickerGrid() {
    LazyVerticalGrid(
        columns         = GridCells.Fixed(4),
        contentPadding  = PaddingValues(12.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(PlaceholderStickers) { (color, name) ->
            val density = LocalDensity.current
            val shape = GenericShape { size, _ ->
                val skew = with(density) { 4.dp.toPx() }
                moveTo(skew, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - skew, size.height)
                lineTo(0f, size.height)
                close()
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(shape)
                    .background(color)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { /* TODO: send sticker */ },
                    ),
            ) {
                Text(
                    text       = name,
                    fontSize   = 11.sp,
                    fontFamily = PersonaFont,
                    color      = Color.Black,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// GIF grid (placeholder)
// ---------------------------------------------------------------------------

@Composable
private fun GifGrid() {
    LazyVerticalGrid(
        columns         = GridCells.Fixed(3),
        contentPadding  = PaddingValues(8.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(PlaceholderGifLabels) { label ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = { /* TODO: send GIF */ },
                    ),
            ) {
                Text(
                    text       = label,
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontFamily = PersonaFont,
                )
            }
        }
    }
}

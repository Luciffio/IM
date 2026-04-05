package com.example.im.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersonaChatSettingsScreen(
    showTimestamps:         Boolean          = true,
    onShowTimestampsChange: (Boolean) -> Unit = {},
    onBack:                 () -> Unit        = {},
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PersonaRed),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(with(density) { topBarShape() })
                    .background(Color.Black),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    BackArrow(onClick = onBack)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text          = "CHAT SETTINGS",
                        fontFamily    = PersonaFont,
                        fontWeight    = FontWeight.Black,
                        fontSize      = 16.sp,
                        color         = Color.White,
                        letterSpacing = 2.sp,
                    )
                }
            }

            // ── Items ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(top = 8.dp),
            ) {
                // "Show send time" toggle row
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .height(50.dp)
                        .background(Color.Black)
                        .padding(horizontal = 12.dp),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text       = "Show send time",
                            fontFamily = PersonaFont,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 14.sp,
                            color      = Color.White.copy(alpha = 0.75f),
                        )
                        P5Toggle(
                            checked          = showTimestamps,
                            onCheckedChange  = onShowTimestampsChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun P5Toggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val bg = if (checked) PersonaRed else Color(0xFF333333)

    Text(
        text       = if (checked) "ON" else "OFF",
        fontFamily = PersonaFont,
        fontWeight = FontWeight.Black,
        fontSize   = 10.sp,
        color      = Color.White,
        modifier   = Modifier
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = { onCheckedChange(!checked) },
            )
            .drawBehind {
                val skew  = 3.dp.toPx()
                val path  = Path().apply {
                    moveTo(skew, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width - skew, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, bg)
                drawPath(path, Color.White, style = Stroke(width = 1.5.dp.toPx()))
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

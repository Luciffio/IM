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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersonaSettingsScreen(
    onBack:              () -> Unit = {},
    onChatSettingsClick: () -> Unit = {},
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
                        text          = "SETTINGS",
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
                SettingsItem("My Account")
                SettingsItem("Notifications and Sounds")
                SettingsItem("Privacy and Security")
                SettingsItem("Chat Settings", clickable = true, onClick = onChatSettingsClick)
                SettingsItem("Folders")
                SettingsItem("Advanced")
                SettingsItem("Speakers and Camera")
                SettingsItem("Battery and Animations")
                SettingsItem("Language", value = "English")
            }
        }
    }
}

@Composable
private fun SettingsItem(
    label:     String,
    value:     String?  = null,
    clickable: Boolean  = false,
    onClick:   () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(50.dp)
            .background(Color.Black)
            .then(
                if (clickable) Modifier.clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onClick,
                ) else Modifier
            )
            .drawBehind {
                // White 2 dp left accent bar for clickable items
                if (clickable) {
                    drawRect(
                        color  = Color.White,
                        size   = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                    )
                }
            }
            .padding(start = if (clickable) 16.dp else 12.dp, end = 12.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = label,
                fontFamily = PersonaFont,
                fontWeight = if (clickable) FontWeight.Black else FontWeight.Normal,
                fontSize   = 14.sp,
                color      = if (clickable) Color.White else Color.White.copy(alpha = 0.75f),
                modifier   = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    text       = value,
                    fontFamily = PersonaFont,
                    fontSize   = 11.sp,
                    color      = Color.White.copy(alpha = 0.5f),
                )
            }
            if (clickable) {
                Text(
                    text       = ">",
                    fontFamily = PersonaFont,
                    fontWeight = FontWeight.Black,
                    fontSize   = 16.sp,
                    color      = PersonaRed,
                )
            }
        }
    }
}
